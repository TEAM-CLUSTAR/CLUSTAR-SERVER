package org.project.domain.memo.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.domain.memo.dto.request.MemoAiCreateRequest;
import org.project.domain.memo.dto.request.MemoCreateRequest;
import org.project.domain.memo.dto.request.MemoPresignedUrlRequest;
import org.project.domain.memo.dto.request.MemoRecommendationRequest;
import org.project.domain.memo.dto.request.MemoUpdateRequest;
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
            summary = "메모 수정",
            description = """
                    메모의 제목/본문/태그와 이미지·파일 첨부를 수정합니다.

                    ## 필드별 반영 방식
                    - **title / content**: 전달된 값으로 전체 교체합니다. (@NotBlank — 빈 값 불가)
                    - **tagNames**: 전달된 목록으로 태그를 전체 교체합니다. 리스트 순서가 곧 우선순위(0번이 최우선)이며,
                      기존에 없던 태그명은 자동 생성됩니다. 빈 배열이면 모든 태그가 제거됩니다.
                    - **images / files**: "바뀐 것"이 아니라 **최종 상태(원하는 결과)** 를 통째로 보냅니다.
                      서버가 현재 저장된 첨부와 비교해 유지/추가/삭제를 스스로 계산합니다.

                    ## 이미지/파일 첨부 편집 규칙 (핵심)
                    각 항목을 아래 셋 중 하나로 표현합니다.
                    - **유지**: `imageId`(또는 `fileId`)만 채워 보냅니다. 상세조회 응답이 s3Key 대신 id를 내려주므로,
                      기존 첨부는 id로 지목합니다. `priority`를 함께 보내면 정렬 순서가 갱신됩니다.
                    - **추가**: id는 비우고 `s3Key`를 채워 보냅니다. 먼저 `POST /api/v1/memo/presigned-urls`로
                      업로드 URL을 발급받아 S3에 올린 뒤, 받은 s3Key와 파일명/용량/확장자/priority를 전달합니다.
                    - **삭제**: 현재 메모엔 있는데 이번 요청 목록에서 빠진 첨부는 자동으로 삭제됩니다.

                    `images` 또는 `files`를 **null로 생략하면 해당 첨부는 전혀 건드리지 않습니다**(부분 수정).
                    반면 **빈 배열([])을 보내면 "첨부 없음"을 의미**하므로 기존 첨부가 모두 삭제됩니다. 주의하세요.

                    ## 제약
                    - 이미지·파일은 각각 최대 5개, 이미지 5MB / 파일 10MB 이하 (유지+추가 합산 기준으로 검증)
                    - 유지 대상 id는 반드시 이 메모의 첨부여야 하며, 새 s3Key는 요청자 소유 경로여야 합니다.

                    ## 처리 흐름 / 반영 시점
                    1. 소유권 검증 → 제목·본문·태그·첨부를 한 트랜잭션에서 반영하고 응답합니다.
                       응답의 `updatedAt`은 이번 수정 시각으로 갱신됩니다. (단순 열람은 updatedAt을 바꾸지 않습니다.)
                    2. 검색/추천용 임베딩 재색인과 삭제된 첨부의 S3 정리는 **커밋 이후 비동기로** 처리됩니다.
                       따라서 "저장 완료 응답 시점"과 "검색 결과에 반영되는 시점"이 다를 수 있습니다.
                       제목·본문이 실제로 바뀐 경우에만 텍스트를 재임베딩합니다(변경 없으면 재색인 생략).
                    """
    )
    @PatchMapping("/{memoId}")
    @BusinessExceptionDescription(SwaggerResponseDescription.UPDATE_MEMO)
    public ResponseEntity<ApiResponse<MemoResponse>> updateMemo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long memoId,
            @Valid @RequestBody MemoUpdateRequest request
    ) {

        Long userId = userDetails.getUserId();

        MemoResponse response = memoService.updateMemo(userId, memoId, request);

        return ResponseEntity.ok(ApiResponse.ok(response));
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
                - tagIds가 있으면 해당 태그가 포함된 메모만 조회합니다.
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
            List<Long> tagIds,

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
                        tagIds,
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
                - tagIds가 있으면 해당 태그가 포함된 메모만 조회합니다.
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
            List<Long> tagIds,

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
                        tagIds,
                        cursorCreatedAt,
                        cursorMemoId,
                        size
                );

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{memoId}")
    @Operation(summary = "메모 상세 조회", description = """
            하나의 메모를 상세조회 합니다.
            AI가 생성한 메모일 경우 선택한 메모의 ID를 리스트로 반환합니다.
            AI가 생성한 메모가 아닐 경우 선택한 메모가 없으므로 빈 리스트를 반환합니다.
            태그는 리스트의 앞부터 우선순위가 높은 순서 입니다.
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
            키워드가 제목/태그/본문에 포함된 모든 메모를 반환합니다.
            정렬: 필드 우선순위(제목 > 태그 > 본문) + 필드 내 온전한 키워드 매칭 우선, 동점은 최신순.
            (의미 기반 검색은 기획 결정으로 비활성화되었습니다.)
            """)
    public ResponseEntity<ApiResponse<MemoSearchResponse>> searchMemos(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String query
    ) {
        MemoSearchResponse response = memoService.searchMemos(userDetails.getUserId(), query);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/recent-viewed")
    @Operation(summary = "최근 열람한 메모", description = """
            검색 모달 [입력 완료 전] 화면용. 사용자가 최근에 열람한 메모를 최신 열람순으로 반환합니다.
            (열람 = 메모 상세조회. 한 번도 열람하지 않은 메모는 포함되지 않습니다.)
            """)
    public ResponseEntity<ApiResponse<MemoRecentViewedResponse>> getRecentViewedMemos(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        MemoRecentViewedResponse response = memoService.getRecentViewedMemos(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/recommendations")
    @Operation(summary = "AI 추천 메모", description = """
            선택한 메모들과 의미적으로 유사한 메모를 최대 3개 추천합니다.
            유사한 메모가 없을 경우 빈 결과와 안내 메시지를 반환합니다.
            """)
    public ResponseEntity<ApiResponse<MemoRecommendationResponse>> recommendMemos(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody MemoRecommendationRequest request
    ) {
        MemoRecommendationResponse response = memoService.recommendMemos(userDetails.getUserId(), request);
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
