package org.project.global.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.domain.memo.dto.response.MemoPresignedUrlResponse;
import org.project.global.exception.domainException.S3CustomException;
import org.project.global.exception.errorcode.S3ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3Util {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * Presigned GET URL 생성 (24시간 유효)
     * @param key S3 Key
     * @return Presigned URL (key가 null이면 null 반환)
     */
    public String generatePresignedUrl(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofHours(24))  //
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest =
                    s3Presigner.presignGetObject(presignRequest);

            String url = presignedRequest.url().toString();
            log.debug("Presigned URL 생성 성공 - Key: {}", key);
            return url;

        } catch (S3Exception e) {
            log.error("AWS S3 Presigned URL 생성 실패 - Key: {}, ErrorCode: {}", key, e.awsErrorDetails().errorCode(), e);
            throw new S3CustomException(S3ErrorCode.PRESIGNED_URL_GENERATION_FAILED);

        } catch (Exception e) {
            log.error("Presigned URL 생성 중 예상치 못한 에러 - Key: {}", key, e);
            throw new S3CustomException(S3ErrorCode.PRESIGNED_URL_GENERATION_FAILED);
        }
    }

    /**
     * S3 파일 삭제
     * @param key S3 Key
     */
    public void deleteFile(String key) {
        if (key == null || key.isEmpty()) {
            log.warn("삭제할 파일 키가 없습니다.");
            return;
        }

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("S3 파일 삭제 성공 - Key: {}", key);

        } catch (S3Exception e) {
            log.error("AWS S3 파일 삭제 실패 - Key: {}, ErrorCode: {}", key, e.awsErrorDetails().errorCode(), e);
            throw new S3CustomException(S3ErrorCode.FILE_DELETE_FAILED);
        } catch (Exception e) {
            log.error("파일 삭제 중 예상치 못한 에러 - Key: {}", key, e);
            throw new S3CustomException(S3ErrorCode.FILE_DELETE_FAILED);
        }
    }

    /**
     * presigned PUT URL 생성
     */
    public MemoPresignedUrlResponse.PresignedUrlResponse createPresignedPutUrl(
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
     * S3 객체 다운로드 (이미지/파일 공용)
     */
    public byte[] download(String s3Key) {

        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes =
                    s3Client.getObjectAsBytes(request);

            return objectBytes.asByteArray();

        } catch (NoSuchKeyException e) {
            log.error("S3에 존재하지 않는 key - Key: {}", s3Key, e);
            throw new S3CustomException(S3ErrorCode.FILE_NOT_FOUND);
        } catch (S3Exception e) {
            log.error("S3 다운로드 실패 - Key: {}, ErrorCode: {}", s3Key, e.awsErrorDetails().errorCode(), e);
            throw new S3CustomException(S3ErrorCode.FILE_DOWNLOAD_FAILED);
        }
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
