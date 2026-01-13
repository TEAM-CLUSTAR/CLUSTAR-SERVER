package org.project.domain.memo.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.domain.memo.dto.request.MemoCreateRequest;
import org.project.domain.memo.dto.response.MemoDetailResponse;
import org.project.domain.memo.dto.response.MemoListDashboardResponse;
import org.project.domain.memo.dto.response.MemoResponse;
import org.project.domain.memo.service.MemoService;
import org.project.domain.user.dto.CustomUserDetails;
import org.project.domain.user.entity.User;
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

    @Operation(summary = "메모 작성", description = "일반 메모를 작성합니다.")
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
            summary = "메모 전체 조회",
            description = """
                    메모를 전체 조회합니다.
                    labelIds가 전달되면 해당 라벨이 포함된 메모만 조회합니다.
                    labelIds가 비어있으면 라벨과 관계없이 전체 조회합니다.
                    """
    )
    @GetMapping
    public ResponseEntity<ApiResponse<MemoListDashboardResponse>> getMemos(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) List<Long> labelIds,
            @RequestParam(required = false) LocalDateTime cursorCreatedAt,
            @RequestParam(required = false) Long cursorMemoId,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        memoService.getMemos(
                                userDetails.getUserId(),
                                labelIds,
                                cursorCreatedAt,
                                cursorMemoId,
                                size
                        )
                )
        );
    }

    @GetMapping("/{memoId}")
    public ResponseEntity<ApiResponse<MemoDetailResponse>> getOneDetailMemo
            (@AuthenticationPrincipal CustomUserDetails userDetails,
             @PathVariable Long memoId
            ) {

        Long userId = userDetails.getUserId();

        MemoDetailResponse response = memoService.getOneMemoDetail(userId, memoId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok(response));

    }

    @DeleteMapping("/{memoId}")
    @Operation(summary = "메모 삭제", description = "특정 메모를 삭제합니다.")
    public ResponseEntity<ApiResponse<Void>> deleteMemo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long memoId
    ) {
        Long userId = userDetails.getUserId();

        memoService.deleteMemo(userId, memoId);

        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
