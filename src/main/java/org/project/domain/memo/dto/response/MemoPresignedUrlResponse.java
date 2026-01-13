package org.project.domain.memo.dto.response;

import java.util.List;

public record MemoPresignedUrlResponse(

        List<PresignedUrlResponse> images,
        List<PresignedUrlResponse> files
) {

    public record PresignedUrlResponse(
            String s3Key,
            String presignedUrl,
            Long bytes,
            String extension,
            Integer priority
    ) {
    }
}
