package org.project.domain.memo.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(
        name = "MemoPresignedUrlRequest",
        description = "메모 이미지/파일 업로드용 presigned URL 발급 요청"
)
public record MemoPresignedUrlRequest(

        @Valid
        @NotNull
        @Schema(
                description = "업로드할 이미지 목록",
                example = """
        [
          {
            "extension": "jpg",
            "bytes": 245678,
            "priority": 1
          },
          {
            "extension": "png",
            "bytes": 532198,
            "priority": 2
          }
        ]
        """
        )
        List<UploadRequest> images,

        @Valid
        @Schema(
                description = "업로드할 파일 목록",
                example = """
        [
          {
            "extension": "pdf",
            "bytes": 1048576,
            "priority": 1
          }
        ]
        """
        )
        @NotNull
        List<UploadRequest> files
) {

    @Schema(
            name = "UploadRequest",
            description = "단일 파일 업로드 정보"
    )
    public record UploadRequest(

            @NotNull
            @Schema(
                    description = "파일 확장자 (확장자만, dot 제외)",
                    example = "jpg"
            )
            String extension,

            @NotNull
            @Schema(
                    description = "파일 크기 (bytes)",
                    example = "245678"
            )
            Long bytes,

            @NotNull
            @Schema(
                    description = "정렬 우선순위 (낮을수록 앞)",
                    example = "1"
            )
            Integer priority
    ) {
    }
}
