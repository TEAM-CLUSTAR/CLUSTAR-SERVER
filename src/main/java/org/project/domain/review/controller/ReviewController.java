package org.project.domain.review.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.project.domain.review.dto.response.ActiveReviewResponse;
import org.project.domain.review.service.ReviewService;
import org.project.domain.user.dto.CustomUserDetails;
import org.project.global.annotation.BusinessExceptionDescription;
import org.project.global.config.swagger.SwaggerResponseDescription;
import org.project.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/reviews")
@RequiredArgsConstructor
@Tag(name = "AI 검토", description = "글의 빈틈을 짚고 질문해, 사용자가 직접 글을 보완하도록 돕는 API")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(
            summary = "현재 검토 조회",
            description = """
                    메모를 열거나 새로고침할 때 호출합니다.
                    - `currentVersion`: 현재 문서 버전(한 번도 검토하지 않았으면 1)
                    - `review`가 null이면 검토 전 또는 종료 후 화면(01)을 보여줍니다
                    - `review`가 있으면 카드 목록·대화·반영 결과로 검토 화면(03~08)을 복원합니다
                    """
    )
    @GetMapping("/active")
    @BusinessExceptionDescription(SwaggerResponseDescription.GET_ACTIVE_REVIEW)
    public ResponseEntity<ApiResponse<ActiveReviewResponse>> getActiveReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long memoId
    ) {
        ActiveReviewResponse response = reviewService.getActiveReview(userDetails.getUserId(), memoId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
