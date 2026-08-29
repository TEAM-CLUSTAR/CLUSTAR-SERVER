package org.project.domain.memo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(requiredProperties = {"images", "files"})
public record MemoPresignedUrlResponse(

        List<PresignedUrlResponse> images,
        List<PresignedUrlResponse> files
) {

    @Schema(requiredProperties = {"s3Key", "presignedUrl", "contentType", "bytes", "extension", "priority"})
    public record PresignedUrlResponse(
            @Schema(description = "업로드 후 메모 저장 시 함께 보낼 S3 key", example = "memo-file/1/uuid.hwp")
            String s3Key,

            @Schema(description = "이 URL로 PUT 업로드한다 (유효시간 10분)")
            String presignedUrl,

            @Schema(
                    description = """
                            업로드(PUT) 시 `Content-Type` 헤더에 **이 값을 그대로** 넣어야 한다.
                            presigned URL 서명에 포함된 값이라, 다른 값을 보내면 S3가 403(SignatureDoesNotMatch)을 반환한다.
                            브라우저가 판단한 타입을 쓰지 말고 서버가 준 값을 그대로 되돌려줄 것.
                            """,
                    example = "application/octet-stream"
            )
            String contentType,

            @Schema(description = "요청에서 받은 파일 크기", example = "102400")
            Long bytes,

            @Schema(description = "요청에서 받은 확장자", example = "hwp")
            String extension,

            @Schema(description = "요청에서 받은 정렬 우선순위", example = "0")
            Integer priority
    ) {
    }
}
