package org.project.domain.ai.rag.A.extract;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.resource.ImageResourceResolver;
import org.project.domain.memo.entity.MemoImage;
import org.project.domain.memo.repository.MemoImageRepository;
import org.springframework.ai.content.Media;
import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MemoImageDocumentReader {

    private final MemoImageRepository memoImageRepository;
    private final ImageResourceResolver imageResourceResolver;
    // S3, local, CDN 등에서 Resource 가져오는 책임 클래스

    public List<Document> read(
            Long memoId,
            List<Long> memoImageIds,
            Long userId
    ) {
        List<MemoImage> images =
                memoImageRepository.findAllById(memoImageIds);

        return images.stream()
                .map(image -> toDocument(memoId, image, userId))
                .toList();
    }

    private Document toDocument(
            Long memoId,
            MemoImage image,
            Long userId
    ) {
        Resource imageResource =
                imageResourceResolver.resolve(image.getImageS3Key());

        MimeType mimeType = switch (image.getImageExtension().toLowerCase()) {
            case "png" -> Media.Format.IMAGE_PNG;
            case "jpg", "jpeg" -> Media.Format.IMAGE_JPEG;
            case "gif" -> Media.Format.IMAGE_GIF;
            case "webp" -> Media.Format.IMAGE_WEBP;
            default -> MimeType.valueOf("image/*");
        };

        Media media = new Media(mimeType, imageResource);

        return new Document(
                media,
                Map.of(
                        "source", "memo-image",
                        "memoId", memoId,
                        "userId", userId,
                        "imageId", image.getId(),
                        "imageKey", image.getImageS3Key(),
                        "extension", image.getImageExtension(),
                        "priority", image.getImagePriority()
                )
        );
    }

}
