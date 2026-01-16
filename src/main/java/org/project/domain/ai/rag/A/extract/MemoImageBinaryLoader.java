package org.project.domain.ai.rag.A.extract;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.event.dto.ImageBinary;
import org.project.domain.memo.entity.MemoImage;
import org.project.domain.memo.repository.MemoImageRepository;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class MemoImageBinaryLoader {

    private final MemoImageRepository memoImageRepository;
    private final S3Client s3Client;
    private final String bucketName = "clustar-bucket-01";

    public ImageBinary load(Long imageId) {

        MemoImage image = memoImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found: " + imageId));

        byte[] bytes;
        try (ResponseInputStream<GetObjectResponse> s3Object =
                     s3Client.getObject(GetObjectRequest.builder()
                             .bucket(bucketName)
                             .key(image.getImageS3Key())
                             .build())) {

            bytes = s3Object.readAllBytes();

        } catch (IOException e) {
            throw new IllegalStateException("Failed to load image from S3", e);
        }

        return new ImageBinary(
                bytes,
                image.getImageS3Key(),
                image.getImageBytes()
        );
    }
}
