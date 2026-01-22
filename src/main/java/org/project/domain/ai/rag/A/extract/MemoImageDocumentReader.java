package org.project.domain.ai.rag.A.extract;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.event.dto.ImageBinary;
import org.project.domain.ai.rag.A.extract.imageExtractor.ImageOcrProcessor;
import org.project.domain.ai.rag.A.extract.imageExtractor.MemoImageBinaryLoader;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.repository.MemoRepository;
import org.project.global.exception.domainException.MemoException;
import org.project.global.exception.errorcode.MemoErrorCode;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MemoImageDocumentReader {

    private static final long MAX_IMAGE_SIZE = 5_000_000L; // 5MB

    private final ImageOcrProcessor ocrProcessor;
    private final MemoImageBinaryLoader memoImageBinaryLoader;
    private final MemoRepository memoRepository;

    public List<Document> read(
            Long memoId,
            List<Long> memoImageIds,
            Long userId
    ) {

        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new MemoException(MemoErrorCode.MEMO_NOT_FOUND));
        LocalDateTime memoCreatedAt = memo.getCreatedAt();

        return memoImageIds.stream()
                .map(imageId -> {

                    ImageBinary image =
                            memoImageBinaryLoader.load(imageId);

                    // 이미지 크기 제한 (OCR 비용 방어)
                    if (image.size() != null && image.size() > MAX_IMAGE_SIZE) {
                        return null;
                    }

                    String ocrText =
                            ocrProcessor.extractText(image.bytes(), image.mimeType());

                    // OCR 실패 이미지 제외
                    if (ocrText == null || ocrText.isBlank()) {
                        return null;
                    }

                    // metadata
                    return Document.builder()
                            .text(ocrText)
                            .metadata(Map.of(
                                    "type", RagDocumentType.MEMO_IMAGE.name(),
                                    "memoId", memoId,
                                    "userId", userId,
                                    "imageId", imageId,
                                    "s3Key", image.s3Key(),
                                    "createdAt", memoCreatedAt.toString()
                            ))
                            .build();
                })
                .filter(doc -> doc != null)
                .toList();
    }
}
