package org.project.global.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.global.exception.domainException.S3CustomException;
import org.project.global.exception.errorcode.S3ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

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
     *
     * @param file 업로드할 파일
     * @param folder S3 폴더 경로 (예: "memo-image", "memo-file")
     * @return S3 Key (예: "memo-image/uuid_filename.jpg")
     */
    public String uploadFile(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new S3CustomException(S3ErrorCode.FILE_EMPTY);
        }

        try {
            // 고유한 파일명 생성
            String originalFilename = file.getOriginalFilename();
            String uuid = UUID.randomUUID().toString();
            String fileName = uuid + "_" + originalFilename;
            String key = folder + "/" + fileName;

            // S3에 업로드
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );

            log.info("S3 파일 업로드 성공 - Key: {}", key);

            return key;

        } catch (IOException e) {
            log.error("파일 읽기 실패", e);
            throw new S3CustomException(S3ErrorCode.FILE_UPLOAD_FAILED);

        } catch (S3Exception e) {  // AWS S3 서비스 에러
            log.error("AWS S3 업로드 실패 - ErrorCode: {}", e.awsErrorDetails().errorCode(), e);
            throw new S3CustomException(S3ErrorCode.FILE_UPLOAD_FAILED);

        } catch (Exception e) {  // 기타 예상치 못한 에러
            log.error("파일 업로드 중 예상치 못한 에러", e);
            throw new S3CustomException(S3ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * Presigned GET URL 생성 (1시간 유효)
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
                    .signatureDuration(Duration.ofHours(1))  // 1시간 유효
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
}
