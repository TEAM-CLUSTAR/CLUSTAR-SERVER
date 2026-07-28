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
import java.util.UUID;

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
     * 이미 embeddedAt이 찍힌(= 새 파이프라인을 거친) 벡터가 있으면 API 호출 없이 스킵한다.
     * 이 판단 하나로 "배치 중단 후 재개"와 "실패한 타입만 선택적 재시도"가 동시에 해결된다 —
     * 둘 다 "이미 끝난 건 다시 안 한다"는 같은 규칙이기 때문.
     */
    private Boolean attemptIfNeeded(Long memoId, String ragType, String failureType, Runnable action) {
        if (vectorStoreRepository.existsEmbeddedDocumentByMemoIdAndType(memoId, ragType, currentEmbeddingModel)) {
            log.debug("[ReEmbedding] {} 이미 현재 모델({})로 임베딩됨 - 스킵: memoId={}", failureType, currentEmbeddingModel, memoId);
            return true;
        }
        return attempt(memoId, failureType, action);
    }

    /**
     * 타입 하나(text/image/file)를 재임베딩 시도한다.
     * 성공하면 해당 타입의 미해결 실패 기록만 resolve하고, 실패하면 해당 타입으로 새로 기록한다.
     * 실패해도 예외를 밖으로 던지지 않아 다른 타입 시도를 막지 않는다.
     */
    private boolean attempt(Long memoId, String type, Runnable action) {
        try {
            action.run();
            embeddingFailureHandler.resolveType(memoId, type);
            return true;
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

    private void reEmbedText(Memo memo) {
        List<UUID> oldIds = vectorStoreRepository.findDocumentIdsByMemoIdAndType(
                memo.getId(), RagDocumentType.MEMO_TEXT.name());

        List<Document> extracted = memoDocumentReader.readText(memo);
        if (extracted.isEmpty()) {
            return;
        }

        List<Document> transformed = memoTextTransformer.transform(extracted);
        if (transformed.isEmpty()) {
            return;
        }

        vectorStoreDocumentLoader.load(transformed);
        vectorStoreRepository.deleteByIds(oldIds);
    }

    private void reEmbedImage(Memo memo, List<Long> imageIds) {
        List<UUID> oldIds = vectorStoreRepository.findDocumentIdsByMemoIdAndType(
                memo.getId(), RagDocumentType.MEMO_IMAGE.name());

        List<Document> extracted = memoImageDocumentReader.read(memo.getId(), imageIds, memo.getUser().getId());
        if (extracted.isEmpty()) {
            return;
        }

        List<Document> transformed = memoImageDocumentTransformer.transform(extracted);
        vectorStoreDocumentLoader.load(transformed);
        vectorStoreRepository.deleteByIds(oldIds);
    }

    private void reEmbedFile(Memo memo, List<Long> fileIds) {
        List<UUID> oldIds = vectorStoreRepository.findDocumentIdsByMemoIdAndType(
                memo.getId(), RagDocumentType.MEMO_FILE.name());

        List<Document> extracted = memoFileDocumentReader.read(memo.getId(), fileIds, memo.getUser().getId());
        if (extracted.isEmpty()) {
            return;
        }

        List<Document> transformed = memoFileDocumentTransformer.transform(extracted);
        vectorStoreDocumentLoader.load(transformed);
        vectorStoreRepository.deleteByIds(oldIds);
    }
}
