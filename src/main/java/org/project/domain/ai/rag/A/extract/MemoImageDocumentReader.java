package org.project.domain.ai.rag.A.extract;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.event.dto.ImageBinary;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MemoImageDocumentReader {

    private static final long MAX_IMAGE_SIZE = 5_000_000L; // 5MB

    private final ImageOcrProcessor ocrProcessor;
    private final MemoImageBinaryLoader memoImageBinaryLoader;

    public List<Document> read(
            Long memoId,
            List<Long> memoImageIds,
            Long userId
    ) {

        return memoImageIds.stream()
                .map(imageId -> {

                    ImageBinary image =
                            memoImageBinaryLoader.load(imageId);

                    // 이미지 크기 제한 (OCR 비용 방어)
                    if (image.size() != null && image.size() > MAX_IMAGE_SIZE) {
                        return null;
                    }

                    String ocrText =
                            ocrProcessor.extractText(image.bytes());

                    // OCR 실패 이미지 제외
                    if (ocrText == null || ocrText.isBlank()) {
                        return null;
                    }

                    // metadata
                    return Document.builder()
                            .text(ocrText)
                            .metadata(Map.of(
                                    "memoId", memoId,
                                    "imageId", imageId,
                                    "userId", userId,
                                    "s3Key", image.s3Key(),
                                    "source", "memo-image"
                            ))
                            .build();
                })
                .filter(doc -> doc != null)
                .toList();
    }
}
