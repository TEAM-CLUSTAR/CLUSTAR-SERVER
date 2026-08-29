package org.project.global.util;

import org.project.global.exception.domainException.MemoException;
import org.project.global.exception.errorcode.MemoErrorCode;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class S3KeyUtil {

    public Long extractUserIdFromS3Key(String s3Key) {
        // memo-image/1/uuid.jpg
        // memo-file/1/uuid.pdf

        if (s3Key == null || s3Key.isBlank()) {
            throw new MemoException(MemoErrorCode.INVALID_S3_KEY_FORMAT);
        }

        try {
            String[] parts = s3Key.split("/");

            if (parts.length < 2) {
                throw new MemoException(MemoErrorCode.INVALID_S3_KEY_FORMAT);
            }

            return Long.parseLong(parts[1]); // index 1 = userId
        } catch (Exception e) {
            throw new MemoException(MemoErrorCode.INVALID_S3_KEY_FORMAT);
        }
    }

    public void validateS3KeyOwner(Long requestUserId, String s3Key) {
        Long ownerId = extractUserIdFromS3Key(s3Key);

        if (!ownerId.equals(requestUserId)) {
            throw new MemoException(MemoErrorCode.S3_KEY_USER_MISMATCH);
        }
    }

    /**
     * s3Key에서 확장자를 추출한다. 키는 서버가 발급할 때 `{prefix}/{userId}/{uuid}.{ext}` 형태로 만들므로,
     * 확장자는 클라이언트가 다시 보내지 않고 여기서 뽑아 쓴다(요청값과 실제 객체가 어긋나는 것을 원천 차단).
     */
    public String extractExtension(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            throw new MemoException(MemoErrorCode.INVALID_S3_KEY_FORMAT);
        }

        String fileName = s3Key.substring(s3Key.lastIndexOf('/') + 1);
        int dotIndex = fileName.lastIndexOf('.');

        // 확장자가 없거나(`uuid`) 점으로 끝나면(`uuid.`) 잘못된 키
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            throw new MemoException(MemoErrorCode.INVALID_S3_KEY_FORMAT);
        }

        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    public void validateS3Key(Long requestUserId, String prefix, String s3Key) {
        validateS3KeyOwner(requestUserId, s3Key);

        if (!s3Key.startsWith(prefix + "/" + requestUserId + "/")) {
            throw new MemoException(MemoErrorCode.INVALID_S3_KEY_FORMAT);
        }
    }
}
