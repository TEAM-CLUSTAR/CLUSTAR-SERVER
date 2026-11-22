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
                    제목/본문/태그/첨부를 **항상 최종 상태로** 보내면 서버가 현재와 비교해 반영합니다. (모든 필드 null 금지 — 빈 건 ""/[])

                    - **title/content**: 값으로 교체 (빈 문자열 허용)
                    - **tagNames**: 목록으로 태그 교체 (순서=우선순위, 없는 이름은 자동 생성, 없으면 [])
                    - **images/files**: 최종 목록 전송 → 서버가 유지/추가/삭제 계산
                      - 유지: `imageId`(또는 `fileId`)만  |  추가: presigned 업로드 후 `s3Key`+메타  |  삭제: 목록에서 빠뜨림
                      - 안 바꿔도 현재 목록 그대로, 없으면 []
                      - 제약: 각 최대 5개, 이미지 5MB/파일 10MB

                    응답의 `updatedAt`은 수정 시각으로 갱신됩니다.
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
                메모를 최근 열람순으로 전체 조회합니다. 미열람 메모는 최하단에 노출됩니다.
                - tagIds가 있으면 해당 태그가 포함된 메모만 조회합니다.
                - 다음 페이지 조회 시 마지막 카드의 lastViewedAt과 memoId를 각각 cursorLastViewedAt, cursorMemoId로 전달합니다.
                - 각 메모는 대표 이미지 1개(presigned URL)와
                  이미지/파일 개수, 생성 시각(createdAt), 마지막 수정 시각(updatedAt)을 포함합니다.
                """
    )
    @GetMapping
    @BusinessExceptionDescription(SwaggerResponseDescription.GET_MEMOS)
    public ResponseEntity<ApiResponse<MemoListDashboardResponse>> getMemos(
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @RequestParam(required = false)
            List<Long> tagIds,

            @RequestParam(required = false)
            LocalDateTime cursorLastViewedAt,

            @RequestParam(required = false)
            Long cursorMemoId,

            @RequestParam(defaultValue = "20")
            int size
    ) {

        MemoListDashboardResponse response =
                memoService.getMemosWithMedia(
                        userDetails.getUserId(),
                        tagIds,
                        cursorLastViewedAt,
                        cursorMemoId,
                        size
                );

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(
            summary = "AI가 생성한 메모 전체 조회(대시보드)",
            description = """
                AI가 생성한 메모를 최근 열람순으로 전체 조회합니다. 미열람 메모는 최하단에 노출됩니다.
                - tagIds가 있으면 해당 태그가 포함된 메모만 조회합니다.
                - 다음 페이지 조회 시 마지막 카드의 lastViewedAt과 memoId를 각각 cursorLastViewedAt, cursorMemoId로 전달합니다.
                - 각 메모는 대표 이미지 1개(presigned URL)와
                  이미지/파일 개수, 생성 시각(createdAt), 마지막 수정 시각(updatedAt)을 포함합니다.
                """
    )
    @GetMapping("/ai")
    @BusinessExceptionDescription(SwaggerResponseDescription.GET_MEMOS)
    public ResponseEntity<ApiResponse<MemoListDashboardResponse>> getAiMemos(
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @RequestParam(required = false)
            List<Long> tagIds,

            @RequestParam(required = false)
            LocalDateTime cursorLastViewedAt,

            @RequestParam(required = false)
            Long cursorMemoId,

            @RequestParam(defaultValue = "20")
            int size
    ) {

        MemoListDashboardResponse response =
                memoService.getAiMemosWithMedia(
                        userDetails.getUserId(),
                        tagIds,
                        cursorLastViewedAt,
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
    @Operation(
            summary = "구조화뷰 메모 전체 조회",
            description = "구조화뷰를 위한 전체 메모를 최근 열람순으로 조회합니다. "
                    + "한 번도 열람하지 않은 메모는 목록 최하단에 노출됩니다."
    )
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
            키워드(텍스트) 검색과 자연어(의미) 검색을 함께 수행합니다.
            1) 텍스트: 키워드가 제목/태그/본문에 포함된 모든 메모 (searchType=TEXT).
               정렬: 필드 우선순위(제목 > 태그 > 본문) + 필드 내 온전한 키워드 매칭 우선, 동점은 최신순.
            2) 의미: 텍스트 결과와 중복되지 않는 의미상 유사 메모를 유사도 순으로 최대 N개 뒤에 덧붙임 (searchType=SEMANTIC).
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
            (열람 = 메모 상세조회.)
            열람 이력이 하나도 없으면 최근 생성 메모(생성 최신순)로 폴백합니다.
            목록 출처는 data.source로 구분합니다. RECENT_VIEWED / RECENT_CREATED
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
    @Operation(summary = "메모 삭제(휴지통으로 이동)", description = """
            메모를 휴지통으로 옮깁니다. 태그와 첨부는 그대로 보관되며 복원할 수 있습니다.
            휴지통에서 30일이 지나면 영구 삭제됩니다.
            삭제 직후 '실행 취소'는 복원 API(PATCH /trash/{memoId}/restore)를 사용합니다.
            """)
    @BusinessExceptionDescription(SwaggerResponseDescription.DELETE_MEMO)
    public ResponseEntity<ApiResponse<Void>> deleteMemo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long memoId
    ) {
        Long userId = userDetails.getUserId();

        memoService.deleteMemo(userId, memoId);

        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // == 휴지통 == //

    @GetMapping("/trash")
    @Operation(summary = "휴지통 목록 조회", description = """
            휴지통에 있는 메모를 휴지통으로 옮긴 최신순으로 조회합니다.
            - totalCount는 휴지통 전체 메모 수입니다(사이드바 개수 표시용).
            - 다음 페이지 조회 시 마지막 카드의 deletedAt과 memoId를 각각 cursorDeletedAt, cursorMemoId로 전달합니다.
            - size는 1~100 (기본 20). 범위를 벗어나면 400입니다.
            """)
    @BusinessExceptionDescription(SwaggerResponseDescription.GET_TRASH_MEMOS)
    public ResponseEntity<ApiResponse<MemoTrashListResponse>> getTrashMemos(
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @RequestParam(required = false)
            LocalDateTime cursorDeletedAt,

            @RequestParam(required = false)
            Long cursorMemoId,

            @RequestParam(defaultValue = "20")
            int size
    ) {
        MemoTrashListResponse response = memoService.getTrashMemos(
                userDetails.getUserId(), cursorDeletedAt, cursorMemoId, size);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/trash/{memoId}")
    @Operation(summary = "휴지통 메모 상세 조회", description = """
            휴지통에 있는 메모를 미리보기용으로 상세 조회합니다. 응답 형식은 메모 상세 조회와 같습니다.
            열람 기록(최근 열람)은 남기지 않습니다.
            """)
    @BusinessExceptionDescription(SwaggerResponseDescription.GET_TRASH_MEMO)
    public ResponseEntity<ApiResponse<MemoDetailResponse>> getTrashMemoDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long memoId
    ) {
        MemoDetailResponse response = memoService.getTrashMemoDetail(userDetails.getUserId(), memoId);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/trash/{memoId}/restore")
    @Operation(summary = "메모 복원", description = """
            휴지통에 있는 메모를 복원합니다. 태그와 첨부도 함께 복원됩니다.
            메모 삭제 직후의 '실행 취소'도 이 API를 사용합니다.
            """)
    @BusinessExceptionDescription(SwaggerResponseDescription.TRASH_MEMO_ACTION)
    public ResponseEntity<ApiResponse<MemoResponse>> restoreMemo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long memoId
    ) {
        MemoResponse response = memoService.restoreMemo(userDetails.getUserId(), memoId);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/trash/{memoId}")
    @Operation(summary = "메모 영구 삭제", description = """
            휴지통에 있는 메모를 영구 삭제합니다. 태그·첨부·S3 파일까지 지워지며 되돌릴 수 없습니다.
            """)
    @BusinessExceptionDescription(SwaggerResponseDescription.TRASH_MEMO_ACTION)
    public ResponseEntity<ApiResponse<Void>> deleteTrashMemoPermanently(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long memoId
    ) {
        memoService.deleteTrashMemoPermanently(userDetails.getUserId(), memoId);

        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @DeleteMapping("/trash")
    @Operation(summary = "휴지통 비우기", description = "휴지통에 있는 내 메모를 모두 영구 삭제합니다. 되돌릴 수 없습니다.")
    public ResponseEntity<ApiResponse<Void>> emptyTrash(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        memoService.emptyTrash(userDetails.getUserId());

        return ResponseEntity.ok(ApiResponse.ok(null));
    }

}
