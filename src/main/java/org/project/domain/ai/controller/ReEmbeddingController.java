package org.project.domain.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.project.domain.ai.dto.response.EmbeddingFailureResponse;
import org.project.domain.ai.dto.response.ReEmbeddingResultResponse;
import org.project.domain.ai.dto.response.ReEmbeddingStartedResponse;
import org.project.domain.ai.service.ReEmbeddingService;
import org.project.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/memos")
@Tag(name = "Admin", description = "임베딩 모델 교체에 따른 재임베딩 관리용 임시 API (관리자와 유저 권한 구분이 안되어있으므로 로그인하여 사용하는 임시 API")
public class ReEmbeddingController {

    private final ReEmbeddingService reEmbeddingService;

    @Operation(
            summary = "전체 메모 재임베딩",
            description = """
            삭제되지 않은 전체 메모를 대상으로 재임베딩 배치를 시작합니다.
            비동기로 실행되며, 호출 즉시 시작 응답을 반환합니다.
            메모별로 기존 벡터는 새 벡터 적재에 성공한 후에만 삭제됩니다.
            """
    )
    @PostMapping("/re-embed")
    public ResponseEntity<ApiResponse<ReEmbeddingStartedResponse>> reEmbedAll() {
        reEmbeddingService.reEmbedAll();
        return ResponseEntity.ok(
                ApiResponse.ok(new ReEmbeddingStartedResponse("재임베딩 배치를 시작했습니다."))
        );
    }

    @Operation(
            summary = "특정 메모 재임베딩(수동 재시도)",
            description = """
            특정 memoId 하나만 동기적으로 재임베딩합니다.
            text/image/file은 서로 독립적으로 시도되어, 하나가 실패해도 나머지는 계속 진행됩니다.
            타입별로 성공한 것만 해당 타입의 미해결 임베딩 실패 기록이 해결 처리됩니다.
            (imageSucceeded/fileSucceeded가 null이면 해당 타입의 첨부가 없어 시도하지 않은 것입니다.)
            """
    )
    @PostMapping("/{memoId}/re-embed")
    public ResponseEntity<ApiResponse<ReEmbeddingResultResponse>> reEmbedOne(@PathVariable Long memoId) {
        return ResponseEntity.ok(ApiResponse.ok(reEmbeddingService.reEmbedOne(memoId)));
    }

    @Operation(
            summary = "미해결 임베딩 실패 목록 조회",
            description = "재시도가 필요한(is_resolved=false) 임베딩 실패 기록을 조회합니다."
    )
    @GetMapping("/embedding-failures")
    public ResponseEntity<ApiResponse<List<EmbeddingFailureResponse>>> getEmbeddingFailures() {
        return ResponseEntity.ok(ApiResponse.ok(reEmbeddingService.getUnresolvedFailures()));
    }
}
