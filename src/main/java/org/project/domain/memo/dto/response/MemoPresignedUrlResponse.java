package org.project.domain.memo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(requiredProperties = {"images", "files"})
public record MemoPresignedUrlResponse(

        List<PresignedUrlResponse> images,
        List<PresignedUrlResponse> files
) {

    @Schema(requiredProperties = {"s3Key", "presignedUrl", "bytes", "extension", "priority"})
    public record PresignedUrlResponse(
            String s3Key,
            String presignedUrl,
            Long bytes,
            String extension,
            Integer priority
    ) {
    }
}
