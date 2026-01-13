package org.project.domain.memo.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record MemoPresignedUrlRequest(

        @NotNull
        List<UploadRequest> images,

        @NotNull
        List<UploadRequest> files
) {

    public record UploadRequest(
            @NotNull String extension,   // jpg, png, pdf ...
            @NotNull Long bytes,         // 파일 크기
            @NotNull Integer priority    // 정렬용
    ) {
    }
}
