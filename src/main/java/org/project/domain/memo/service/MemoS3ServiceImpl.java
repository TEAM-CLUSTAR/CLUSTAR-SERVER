package org.project.domain.memo.service;

import lombok.RequiredArgsConstructor;
import org.project.domain.memo.dto.request.MemoPresignedUrlRequest;
import org.project.domain.memo.dto.response.MemoPresignedUrlResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemoS3ServiceImpl implements MemoS3Service {

    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Override
    public MemoPresignedUrlResponse issuePresignedUrls(
            Long userId,
            MemoPresignedUrlRequest request
    ) {

        List<MemoPresignedUrlResponse.PresignedUrlResponse> imageUrls =
                request.images().stream()
                        .map(r -> createPresignedPutUrl(
                                userId,
                                "memo-image",
                                r.extension(),
                                r.bytes(),
                                r.priority()
                        ))
                        .toList();

        List<MemoPresignedUrlResponse.PresignedUrlResponse> fileUrls =
                request.files().stream()
                        .map(r -> createPresignedPutUrl(
                                userId,
                                "memo-file",
                                r.extension(),
                                r.bytes(),
                                r.priority()
                        ))
                        .toList();

        return new MemoPresignedUrlResponse(imageUrls, fileUrls);
    }

    /**
     * presigned PUT URL 생성
     */
    private MemoPresignedUrlResponse.PresignedUrlResponse createPresignedPutUrl(
            Long userId,
            String prefix,
            String extension,
            Long bytes,
            Integer priority
    ) {

        String s3Key = String.format(
                "%s/%d/%s.%s",
                prefix,
                userId,
                UUID.randomUUID(),
                extension
        );

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .contentType(resolveContentType(extension))
                .build();

        PutObjectPresignRequest presignRequest =
                PutObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(10)) // 유효시간 10분
                        .putObjectRequest(putObjectRequest)
                        .build();

        String presignedUrl =
                s3Presigner.presignPutObject(presignRequest)
                        .url()
                        .toString();

        return new MemoPresignedUrlResponse.PresignedUrlResponse(
                s3Key,
                presignedUrl,
                bytes,
                extension,
                priority
        );
    }

    /**
     * 확장자 → Content-Type 매핑
     */
    private String resolveContentType(String extension) {
        return switch (extension.toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "pdf" -> "application/pdf";
            case "txt" -> "text/plain";
            default -> "application/octet-stream";
        };
    }
}
