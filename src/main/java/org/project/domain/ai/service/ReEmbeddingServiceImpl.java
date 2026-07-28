package org.project.domain.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.domain.ai.dto.response.EmbeddingFailureResponse;
import org.project.domain.ai.dto.response.ReEmbeddingResultResponse;
import org.project.domain.ai.event.EmbeddingFailureHandler;
import org.project.domain.ai.rag.A.extract.MemoDocumentReader;
import org.project.domain.ai.rag.A.extract.MemoFileDocumentReader;
import org.project.domain.ai.rag.A.extract.MemoImageDocumentReader;
import org.project.domain.ai.rag.A.extract.RagDocumentType;
import org.project.domain.ai.rag.B.transform.file.MemoFileDocumentTransformer;
import org.project.domain.ai.rag.B.transform.image.MemoImageDocumentTransformer;
import org.project.domain.ai.rag.B.transform.text.MemoTextTransformer;
import org.project.domain.ai.rag.C.load.VectorStoreDocumentLoader;
import org.project.domain.ai.repository.EmbeddingFailureRepository;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.MemoFile;
import org.project.domain.memo.entity.MemoImage;
import org.project.domain.memo.repository.MemoFileRepository;
import org.project.domain.memo.repository.MemoImageRepository;
import org.project.domain.memo.repository.MemoRepository;
import org.project.domain.memo.repository.VectorStoreRepository;
import org.project.global.exception.domainException.MemoException;
import org.project.global.exception.errorcode.MemoErrorCode;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReEmbeddingServiceImpl implements ReEmbeddingService {

    private final MemoRepository memoRepository;
    private final MemoImageRepository memoImageRepository;
    private final MemoFileRepository memoFileRepository;

    private final MemoDocumentReader memoDocumentReader;
    private final MemoTextTransformer memoTextTransformer;
    private final MemoImageDocumentReader memoImageDocumentReader;
    private final MemoImageDocumentTransformer memoImageDocumentTransformer;
    private final MemoFileDocumentReader memoFileDocumentReader;
    private final MemoFileDocumentTransformer memoFileDocumentTransformer;
    private final VectorStoreDocumentLoader vectorStoreDocumentLoader;
    private final VectorStoreRepository vectorStoreRepository;

    private final EmbeddingFailureHandler embeddingFailureHandler;
    private final EmbeddingFailureRepository embeddingFailureRepository;

    // 스킵 판단 기준 - 이 값과 vector_store의 embeddingModel이 다르면 구버전으로 보고 재임베딩 대상에 포함시킨다.
    @Value("${spring.ai.google.genai.embedding.text.options.model}")
    private String currentEmbeddingModel;

    @Async
    @Override
    public void reEmbedAll() {
        List<Long> memoIds = memoRepository.findAllNotDeletedMemoIds();
        log.info("[ReEmbedding] 배치 시작 - 대상 메모 수: {}", memoIds.size());

        for (Long memoId : memoIds) {
            try {
                // 타입(text/image/file)별 성공/실패 기록은 reEmbedOne 내부에서 개별 처리된다.
                reEmbedOne(memoId);
            } catch (Exception e) {
                // 여기 도달하는 건 타입별 임베딩 이전 단계(메모 조회 등)의 실패뿐이다.
                log.error("[ReEmbedding] 메모 단위 실패: memoId={}", memoId, e);
                embeddingFailureHandler.record(memoId, "memo", e);
            }
        }

        log.info("[ReEmbedding] 배치 종료");
    }

    @Override
    public ReEmbeddingResultResponse reEmbedOne(Long memoId) {
        Memo memo = memoRepository.findByIdWithUserAndNotDeleted(memoId)
                .orElseThrow(() -> new MemoException(MemoErrorCode.MEMO_NOT_FOUND));

        // 텍스트는 항상 시도 대상. 이미지/파일은 하나가 실패해도 나머지 타입은 계속 시도한다(서로 독립).
        Boolean textSucceeded = attemptIfNeeded(
                memoId, RagDocumentType.MEMO_TEXT.name(), "text", () -> reEmbedText(memo));

        List<Long> imageIds = memoImageRepository.findByMemoIdIn(List.of(memoId)).stream()
                .map(MemoImage::getId)
                .toList();
        Boolean imageSucceeded = imageIds.isEmpty()
                ? null
                : attemptIfNeeded(memoId, RagDocumentType.MEMO_IMAGE.name(), "image",
                        () -> reEmbedImage(memo, imageIds));

        List<Long> fileIds = memoFileRepository.findByMemoIdIn(List.of(memoId)).stream()
                .map(MemoFile::getId)
                .toList();
        Boolean fileSucceeded = fileIds.isEmpty()
                ? null
                : attemptIfNeeded(memoId, RagDocumentType.MEMO_FILE.name(), "file",
                        () -> reEmbedFile(memo, fileIds));

        return new ReEmbeddingResultResponse(memoId, textSucceeded, imageSucceeded, fileSucceeded);
    }

    /**
     * "이미 끝났다"고 스킵하려면 두 조건을 다 만족해야 한다:
     * 1) 현재 모델로 임베딩된 벡터가 있다 (모델 교체 감지용)
     * 2) 이 타입에 미해결 실패 기록이 없다 (부분 성공 감지용 — image/file은 여러 문서로 이뤄져 있어서
     *    "현재 모델 벡터가 하나라도 있다"만으로는 나머지 문서가 실패한 채로 방치될 수 있기 때문)
     * 즉 벡터 존재 여부만으로 판단하지 않고, 실제 완료 여부(미해결 실패 없음)까지 같이 봐야 한다.
     */
    private Boolean attemptIfNeeded(Long memoId, String ragType, String failureType, Supplier<Boolean> action) {
        boolean upToDate = vectorStoreRepository.existsEmbeddedDocumentByMemoIdAndType(memoId, ragType, currentEmbeddingModel);
        boolean hasUnresolvedFailure = embeddingFailureRepository.existsByMemoIdAndEmbeddingTypeAndIsResolvedFalse(memoId, failureType);

        if (upToDate && !hasUnresolvedFailure) {
            log.debug("[ReEmbedding] {} 이미 현재 모델({})로 완전히 임베딩됨 - 스킵: memoId={}", failureType, currentEmbeddingModel, memoId);
            return true;
        }
        return attempt(memoId, failureType, action);
    }

    /**
     * 타입 하나(text/image/file)를 재임베딩 시도한다.
     * action은 "실제로 새 벡터를 온전히 채워 넣고 기존 벡터 정리까지 끝냈는지"를 boolean으로 리턴해야 한다
     * (문서 존재 여부나 예외 미발생만으로 완료를 판단하지 않기 위함).
     * true(완전 성공)일 때만 resolve하고, false(빈 결과/부분 성공)나 예외는 전부 실패로 기록한다.
     * 실패해도 예외를 밖으로 던지지 않아 다른 타입 시도를 막지 않는다.
     */
    private boolean attempt(Long memoId, String type, Supplier<Boolean> action) {
        try {
            boolean completed = action.get();
            if (completed) {
                embeddingFailureHandler.resolveType(memoId, type);
            } else {
                log.warn("[ReEmbedding] {} 불완전한 결과(빈 추출/변환 또는 일부 문서만 처리됨): memoId={}", type, memoId);
                embeddingFailureHandler.record(memoId, type,
                        new IllegalStateException("추출/변환 결과가 없거나 일부 문서만 처리됨"));
            }
            return completed;
        } catch (Exception e) {
            log.error("[ReEmbedding] {} 실패: memoId={}", type, memoId, e);
            embeddingFailureHandler.record(memoId, type, e);
            return false;
        }
    }

    @Override
    public List<EmbeddingFailureResponse> getUnresolvedFailures() {
        return embeddingFailureRepository.findByIsResolvedFalse().stream()
                .map(EmbeddingFailureResponse::from)
                .toList();
    }

    /* =========================
       메모별 재임베딩 (타입별로 old id 캡처 -> 신규 적재 -> old 삭제)
       ========================= */

    /**
     * @return 새 텍스트 벡터를 온전히 채워 넣었으면 true. 추출/변환 결과가 비어있으면 기존 벡터를
     *         전혀 건드리지 않고 false를 반환한다(빈 결과를 성공으로 오인해 기존 데이터를 지우지 않기 위함).
     */
    private boolean reEmbedText(Memo memo) {
        List<UUID> oldIds = vectorStoreRepository.findDocumentIdsByMemoIdAndType(
                memo.getId(), RagDocumentType.MEMO_TEXT.name());

        List<Document> extracted = memoDocumentReader.readText(memo);
        if (extracted.isEmpty()) {
            return false;
        }

        List<Document> transformed = memoTextTransformer.transform(extracted);
        if (transformed.isEmpty()) {
            return false;
        }

        vectorStoreDocumentLoader.load(transformed);
        vectorStoreRepository.deleteByIds(oldIds);
        return true;
    }

    /**
     * @return 요청한 이미지 전부(imageIds 전체)가 문서로 추출되어 새로 적재됐을 때만 true.
     *         일부 이미지만 OCR에 성공한 경우(나머지는 예외 없이 조용히 걸러짐)에도 그 일부는 적재하되,
     *         "완전히 끝난 것"은 아니므로 false를 반환해 미해결 실패로 계속 추적되게 한다.
     *         추출/변환 결과가 아예 비었으면 기존 벡터를 건드리지 않는다.
     */
    private boolean reEmbedImage(Memo memo, List<Long> imageIds) {
        List<UUID> oldIds = vectorStoreRepository.findDocumentIdsByMemoIdAndType(
                memo.getId(), RagDocumentType.MEMO_IMAGE.name());

        List<Document> extracted = memoImageDocumentReader.read(memo.getId(), imageIds, memo.getUser().getId());
        if (extracted.isEmpty()) {
            return false;
        }

        List<Document> transformed = memoImageDocumentTransformer.transform(extracted);
        if (transformed.isEmpty()) {
            return false;
        }

        vectorStoreDocumentLoader.load(transformed);
        vectorStoreRepository.deleteByIds(oldIds);

        Set<Object> succeededImageIds = extracted.stream()
                .map(doc -> doc.getMetadata().get("imageId"))
                .collect(Collectors.toSet());
        return succeededImageIds.size() == imageIds.size();
    }

    /**
     * @return 요청한 파일 전부(fileIds 전체)가 문서로 추출되어 새로 적재됐을 때만 true.
     *         파일 하나가 Tika에서 문서 여러 개로 쪼개질 수 있어 문서 개수가 아니라
     *         "성공한 fileId의 종류 수"로 완전성을 판단한다.
     */
    private boolean reEmbedFile(Memo memo, List<Long> fileIds) {
        List<UUID> oldIds = vectorStoreRepository.findDocumentIdsByMemoIdAndType(
                memo.getId(), RagDocumentType.MEMO_FILE.name());

        List<Document> extracted = memoFileDocumentReader.read(memo.getId(), fileIds, memo.getUser().getId());
        if (extracted.isEmpty()) {
            return false;
        }

        List<Document> transformed = memoFileDocumentTransformer.transform(extracted);
        if (transformed.isEmpty()) {
            return false;
        }

        vectorStoreDocumentLoader.load(transformed);
        vectorStoreRepository.deleteByIds(oldIds);

        Set<Object> succeededFileIds = extracted.stream()
                .map(doc -> doc.getMetadata().get("fileId"))
                .collect(Collectors.toSet());
        return succeededFileIds.size() == fileIds.size();
    }
}
