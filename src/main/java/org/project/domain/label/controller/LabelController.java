package org.project.domain.label.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.domain.label.dto.request.LabelCreateRequest;
import org.project.domain.label.dto.request.LabelUpdateRequest;
import org.project.domain.label.dto.response.LabelHierarchyResponse;
import org.project.domain.label.dto.response.LabelListResponse;
import org.project.domain.label.dto.response.LabelParentListResponse;
import org.project.domain.label.dto.response.LabelSummaryResponse;
import org.project.domain.label.service.LabelService;
import org.project.domain.user.dto.CustomUserDetails;
import org.project.global.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/label")
@Tag(name = "라벨", description = "라벨 관련 API")
public class LabelController {

    private final LabelService labelService;

    @Operation(
            summary = "라벨 전체 조회",
            description = """
            사용자가 생성한 모든 라벨 목록을 조회합니다.
            메모에 사용된 라벨과 미사용 라벨을 모두 포함합니다.
            """
    )
    @GetMapping
    public ResponseEntity<ApiResponse<LabelListResponse>> getAllLabels(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        Long userId = userDetails.getUserId();

        LabelListResponse response =
                labelService.getAllLabels(userId);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(
            summary = "태그 생성",
            description = """
            태그를 생성합니다.
            parentLabelId가 있으면 하위 태그로 생성하고, 없으면 부모 태그로 생성합니다.
            """
    )
    @PostMapping
    public ResponseEntity<ApiResponse<LabelSummaryResponse>> createLabel(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody LabelCreateRequest request
    ) {
        Long userId = userDetails.getUserId();

        LabelSummaryResponse response = labelService.createLabel(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    @Operation(
            summary = "태그 수정",
            description = """
            태그 이름을 수정합니다.
            """
    )
    @PutMapping("/{labelId}")
    public ResponseEntity<ApiResponse<LabelSummaryResponse>> updateLabel(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long labelId,
            @Valid @RequestBody LabelUpdateRequest request
    ) {
        Long userId = userDetails.getUserId();

        LabelSummaryResponse response = labelService.updateLabel(userId, labelId, request);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(
            summary = "태그 삭제",
            description = """
            태그를 삭제합니다.
            태그에 연결된 메모-태그 관계도 함께 정리합니다.
            """
    )
    @DeleteMapping("/{labelId}")
    public ResponseEntity<ApiResponse<String>> deleteLabel(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long labelId
    ) {
        Long userId = userDetails.getUserId();

        labelService.deleteLabel(userId, labelId);

        return ResponseEntity.ok(ApiResponse.ok("태그가 삭제되었습니다."));
    }

    @Operation(
            summary = "부모 태그 최대 10개 조회",
            description = """
            사용자의 부모 태그 최대 10개를 생성일 내림차순으로 조회합니다.
            """
    )
    @GetMapping("/parents")
    public ResponseEntity<ApiResponse<LabelParentListResponse>> getParentLabels(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();

        LabelParentListResponse response = labelService.getParentLabels(userId);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(
            summary = "부모 태그 기반 하위 태그 조회",
            description = """
            부모 태그를 기준으로 자식 태그와 손자 태그를 계층 구조로 조회합니다.
            """
    )
    @GetMapping("/parents/{parentLabelId}/children")
    public ResponseEntity<ApiResponse<LabelHierarchyResponse>> getChildAndGrandChildLabels(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long parentLabelId
    ) {
        Long userId = userDetails.getUserId();

        LabelHierarchyResponse response = labelService.getChildAndGrandChildLabels(userId, parentLabelId);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
