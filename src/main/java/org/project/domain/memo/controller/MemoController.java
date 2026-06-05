package org.project.domain.memo.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.domain.memo.dto.request.MemoAiCreateRequest;
import org.project.domain.memo.dto.request.MemoCreateRequest;
import org.project.domain.memo.dto.request.MemoPresignedUrlRequest;
import org.project.domain.memo.dto.response.*;
import org.project.domain.memo.service.MemoService;
import org.project.domain.user.dto.CustomUserDetails;
import org.project.global.annotation.BusinessExceptionDescription;
import org.project.global.config.swagger.SwaggerResponseDescription;
import org.project.global.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/memo")
@Tag(name = "메모", description = "메모 작성, 검색 등 API")
public class MemoController {

    private final MemoService memoService;

    @Operation(
            summary = "메모 이미지/파일 presigned URL 발급",
            description = """
                    메모 생성 전에 S3에 업로드할 이미지/파일용 presigned PUT URL을 발급합니다.
                    업로드 완료 후 s3Key를 메모 생성 API에 전달해야 합니다.
                    """
    )
    @PostMapping("/presigned-urls")
    @BusinessExceptionDescription(SwaggerResponseDescription.GET_PRESIGNED_URLS)
    public ResponseEntity<ApiResponse<MemoPresignedUrlResponse>> issuePresignedUrls(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody MemoPresignedUrlRequest request
    ) {

        Long userId = userDetails.getUserId();

        MemoPresignedUrlResponse response =
                memoService.issuePresignedUrls(userId, request);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(
            summary = "메모 작성",
            description = """
                메모를 작성합니다.
                이미지/파일은 presigned URL을 통해 S3에 업로드 완료 후
                s3Key 정보를 함께 전달해야 합니다.
                """
    )
    @PostMapping
    @BusinessExceptionDescription(SwaggerResponseDescription.CREATE_MEMO)
    public ResponseEntity<ApiResponse<MemoResponse>> createMemo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody MemoCreateRequest request
    ) {

        Long userId = userDetails.getUserId();

        MemoResponse response = memoService.createMemo(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    @Operation(
            summary = "AI가 만든 메모 저장",
            description = """
                AI 응답 결과를 기반으로 메모를 등록요청을 하는 API입니다.
                제목과 본문을 분리해서 전달해야 합니다.
                """
    )
    @PostMapping("/ai")
    @BusinessExceptionDescription(SwaggerResponseDescription.CREATE_MEMO)
    public ResponseEntity<ApiResponse<MemoResponse>> createAiMemo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody MemoAiCreateRequest request
    ) {

        Long userId = userDetails.getUserId();

        MemoResponse response = memoService.createAiMemo(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    @Operation(
            summary = "메모 전체 조회(대시보드)",
            description = """
                메모를 전체 조회합니다.
                - labelIds가 있으면 해당 라벨이 포함된 메모만 조회합니다.
                - 커서 기반 페이지네이션을 지원합니다.
                - 각 메모는 대표 이미지 1개(presigned URL)와
                  이미지/파일 개수 정보를 포함합니다.
                """
    )
    @GetMapping
    @BusinessExceptionDescription(SwaggerResponseDescription.GET_MEMOS)
    public ResponseEntity<ApiResponse<MemoListDashboardResponse>> getMemos(
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @RequestParam(required = false)
            List<Long> labelIds,

            @RequestParam(required = false)
            LocalDateTime cursorCreatedAt,

            @RequestParam(required = false)
            Long cursorMemoId,

            @RequestParam(defaultValue = "20")
            int size
    ) {

        MemoListDashboardResponse response =
                memoService.getMemosWithMedia(
                        userDetails.getUserId(),
                        labelIds,
                        cursorCreatedAt,
                        cursorMemoId,
                        size
                );

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(
            summary = "AI가 생성한 메모 전체 조회(대시보드)",
            description = """
                AI가 생성한 메모를 전체 조회합니다. 
                - labelIds가 있으면 해당 라벨이 포함된 메모만 조회합니다.
                - 커서 기반 페이지네이션을 지원합니다.
                - 각 메모는 대표 이미지 1개(presigned URL)와
                  이미지/파일 개수 정보를 포함합니다.
                """
    )
    @GetMapping("/ai")
    @BusinessExceptionDescription(SwaggerResponseDescription.GET_MEMOS)
    public ResponseEntity<ApiResponse<MemoListDashboardResponse>> getAiMemos(
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @RequestParam(required = false)
            List<Long> labelIds,

            @RequestParam(required = false)
            LocalDateTime cursorCreatedAt,

            @RequestParam(required = false)
            Long cursorMemoId,

            @RequestParam(defaultValue = "20")
            int size
    ) {

        MemoListDashboardResponse response =
                memoService.getAiMemosWithMedia(
                        userDetails.getUserId(),
                        labelIds,
                        cursorCreatedAt,
                        cursorMemoId,
                        size
                );

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{memoId}")
    @Operation(summary = "메모 전체 조회(모달창)", description = """
            하나의 메모를 상세조회 합니다.
            AI가 생성한 메모일 경우 선택한 메모의 ID를 리스트로 반환합니다.
            AI가 생성한 메모가 아닐 경우 선택한 메모가 없으므로 빈 리스트를 반환합니다.
            라벨은 리스트의 앞부터 우선순위가 높은 순서 입니다.
            """)
    @BusinessExceptionDescription(SwaggerResponseDescription.GET_ONE_MEMO)
    public ResponseEntity<ApiResponse<MemoDetailResponse>> getOneDetailMemo
            (@AuthenticationPrincipal CustomUserDetails userDetails,
             @PathVariable Long memoId
            ) {

        Long userId = userDetails.getUserId();

        MemoDetailResponse response = memoService.getOneMemoDetail(userId, memoId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok(response));

    }

    @GetMapping("/structure")
    @Operation(summary = "구조화뷰 메모 전체 조회", description = "구조화뷰를 위한 전체 메모를 조회합니다.")
    public ResponseEntity<ApiResponse<MemoStructureListResponse>> getStructureMemo(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        Long userId = userDetails.getUserId();

        MemoStructureListResponse response = memoService.getStructureMemo(userId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok(response));

    }

    @GetMapping("/search")
    @Operation(summary = "메모 검색", description = """
            검색어를 입력하면 최대 5개의 메모를 반환합니다.
            - 텍스트 매칭(제목/본문/라벨) 최대 3개
            - 의미 기반 벡터 검색 최대 2개
            """)
    public ResponseEntity<ApiResponse<MemoSearchResponse>> searchMemos(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String query
    ) {
        MemoSearchResponse response = memoService.searchMemos(userDetails.getUserId(), query);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{memoId}")
    @Operation(summary = "메모 삭제", description = "특정 메모를 삭제합니다.")
    @BusinessExceptionDescription(SwaggerResponseDescription.DELETE_MEMO)
    public ResponseEntity<ApiResponse<Void>> deleteMemo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long memoId
    ) {
        Long userId = userDetails.getUserId();

        memoService.deleteMemo(userId, memoId);

        return ResponseEntity.ok(ApiResponse.ok(null));
    }

}
