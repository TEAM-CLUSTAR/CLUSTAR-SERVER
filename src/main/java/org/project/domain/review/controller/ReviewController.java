package org.project.domain.review.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.domain.review.dto.request.ReviewAnswerRequest;
import org.project.domain.review.dto.request.ReviewApplyRequest;
import org.project.domain.review.dto.response.ApplyResponse;
import org.project.domain.review.dto.response.EndReviewResponse;
import org.project.domain.review.dto.request.ReviewStartRequest;
import org.project.domain.review.dto.response.ActiveReviewResponse;
import org.project.domain.review.dto.response.AnswerResponse;
import org.project.domain.review.dto.response.RefineResponse;
import org.project.domain.review.dto.response.ReviewResponse;
import org.project.domain.review.service.ReviewFindingService;
import org.project.domain.review.service.ReviewService;
import org.project.domain.user.dto.CustomUserDetails;
import org.project.global.annotation.BusinessExceptionDescription;
import org.project.global.config.swagger.SwaggerResponseDescription;
import org.project.global.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/reviews")
@RequiredArgsConstructor
@Tag(name = "AI 검토", description = "글의 빈틈을 짚고 질문해, 사용자가 직접 글을 보완하도록 돕는 API")
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewFindingService findingService;

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

    @Operation(
            summary = "검토 시작",
            description = """
                    [검토하기] 버튼. AI가 본문 전체를 읽고 빈틈 카드를 만들어 새 버전으로 저장합니다.
                    - 본문(제목 제외, 공백 포함)이 300자 이상이어야 합니다
                    - 진행 중인 검토가 있으면 409, AI 호출이 실패하면 503(아무것도 저장되지 않음)
                    - AI 분석이 끝날 때까지 응답을 기다립니다(수 초). 요청 전에 본문 자동 저장을 먼저 끝내 주세요
                    - 응답의 `version`이 새 문서 버전(첫 검토는 2), `findings`가 비어 있으면 짚을 곳이 없는 것입니다
                    """
    )
    @PostMapping
    @BusinessExceptionDescription(SwaggerResponseDescription.START_REVIEW)
    public ResponseEntity<ApiResponse<ReviewResponse>> startReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ReviewStartRequest request
    ) {
        ReviewResponse response = reviewService.startReview(userDetails.getUserId(), request.memoId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @Operation(
            summary = "다시 검토",
            description = """
                    [다시 검토하기] 버튼. 반영한 문장이 들어간 지금 본문으로 다시 분석해 새 버전을 만듭니다.
                    - 지금 버전은 종료로 남고(기록 보존), 응답의 `reviewId`가 새로 바뀝니다. 이후 요청은 새 ID로 보내 주세요
                    - 반영하지 않은 카드는 AI 결과와 상관없이 대화째 그대로 옮겨집니다(답하던 내용 유지)
                    - 반영한 카드는 목록에서 빠지고, AI가 찾은 새 빈틈이 추가됩니다. 번호는 본문 순서로 다시 매깁니다
                    - AI가 실패하면 503이고 지금 버전은 그대로 남습니다
                    """
    )
    @PostMapping("/{reviewId}/rerun")
    @BusinessExceptionDescription(SwaggerResponseDescription.RERUN_REVIEW)
    public ResponseEntity<ApiResponse<ReviewResponse>> rerunReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long reviewId
    ) {
        ReviewResponse response = reviewService.rerunReview(userDetails.getUserId(), reviewId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @Operation(
            summary = "검토 종료",
            description = """
                    [검토 종료] 버튼. 진행 중 검토를 끝내고 01 기본 화면으로 돌아갑니다.
                    - 반영한 문장은 이미 본문에 들어가 있으므로 따로 합칠 것이 없습니다(반영 블록 표시만 없애면 됩니다)
                    - 반영하지 않은 카드와 답은 화면에서 사라지지만, 버전 기록으로는 남습니다
                    - 응답의 `currentVersion`을 '문서 vN'에 쓰고, 4-t 안내 문구('검토를 마쳤어요…')는 프론트가 띄웁니다
                    - 없거나 이미 종료된 검토면 404
                    """
    )
    @PostMapping("/{reviewId}/end")
    @BusinessExceptionDescription(SwaggerResponseDescription.END_REVIEW)
    public ResponseEntity<ApiResponse<EndReviewResponse>> endReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long reviewId
    ) {
        EndReviewResponse response = reviewService.endReview(userDetails.getUserId(), reviewId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(
            summary = "답 보내기",
            description = """
                    빈틈 카드 입력창의 ↑ 버튼. 내 답을 저장하고 카드를 ANSWERING으로 바꿉니다.
                    - 첫 답이면 AI가 후속 질문을 만들어 `aiMessage`로 함께 돌려줍니다(수 초 소요)
                    - 두 번째 답부터는 `aiMessage`가 null입니다. 이때 프론트가 고정 안내 문구를 보여줍니다
                    - 후속 질문 생성이 실패해도 답은 저장되고 `aiMessage`만 null로 옵니다(에러 아님)
                    - 답은 1000자 이하, 이미 반영한 카드면 409
                    """
    )
    @PostMapping("/{reviewId}/findings/{findingId}/answers")
    @BusinessExceptionDescription(SwaggerResponseDescription.ANSWER_FINDING)
    public ResponseEntity<ApiResponse<AnswerResponse>> answer(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long reviewId,
            @PathVariable Long findingId,
            @Valid @RequestBody ReviewAnswerRequest request
    ) {
        AnswerResponse response = findingService.answer(userDetails.getUserId(), reviewId, findingId, request.content());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(
            summary = "문장 다듬기",
            description = """
                    카드 아래 [반영하기] 버튼. 지금까지 보낸 답만 재료로, 문서에 넣을 문장으로 다듬어 돌려줍니다.
                    - `refinedText`: 질문마다 "라벨: 문장" 한 줄씩. 본문의 짚은 문장 아래 미리보기 편집창에 넣습니다
                    - `rawText`: 내 답 원문. '원문 비교'에 보여줍니다
                    - 서버에 저장하지 않습니다. [취소]는 서버 호출 없이 화면만 닫으면 됩니다
                    - 답이 없으면 400, AI 실패 시 503([다시 시도] 표시. [원문 그대로 넣기]는 계속 가능)
                    """
    )
    @PostMapping("/{reviewId}/findings/{findingId}/refine")
    @BusinessExceptionDescription(SwaggerResponseDescription.REFINE_FINDING)
    public ResponseEntity<ApiResponse<RefineResponse>> refine(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long reviewId,
            @PathVariable Long findingId
    ) {
        RefineResponse response = findingService.refine(userDetails.getUserId(), reviewId, findingId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(
            summary = "문서에 반영",
            description = """
                    미리보기의 [문서에 반영](type=REFINED) / [원문 그대로 넣기](type=RAW) 버튼.
                    짚은 문장이 있는 줄 바로 다음 줄에 문장을 넣고 카드를 APPLIED로 바꿉니다.
                    - REFINED: `text`에 미리보기에서 고친 최종 문장을 보냅니다(비어 있으면 400)
                    - RAW: `text` 없이 보내면 서버가 저장된 내 답을 그대로 넣습니다(다듬기가 실패했어도 가능)
                    - 응답의 `content`(반영 후 본문 전체)로 **편집기 내용을 반드시 바꿔 주세요.** 자동 저장이 옛 본문으로 덮어쓰면 반영 문장이 사라집니다
                    - 본문이 바뀌어 짚은 문장을 못 찾으면 409, 이미 반영한 카드면 409
                    """
    )
    @PostMapping("/{reviewId}/findings/{findingId}/apply")
    @BusinessExceptionDescription(SwaggerResponseDescription.APPLY_FINDING)
    public ResponseEntity<ApiResponse<ApplyResponse>> apply(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long reviewId,
            @PathVariable Long findingId,
            @Valid @RequestBody ReviewApplyRequest request
    ) {
        ApplyResponse response = findingService.apply(
                userDetails.getUserId(), reviewId, findingId, request.type(), request.text());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
