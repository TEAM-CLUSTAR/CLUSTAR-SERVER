package org.project.global.util;

import org.project.global.exception.domainException.MemoException;
import org.project.global.exception.errorcode.MemoErrorCode;
import org.springframework.stereotype.Component;

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
}
