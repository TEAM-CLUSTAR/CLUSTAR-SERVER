package org.project.domain.label.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.project.domain.label.dto.response.LabelHierarchyResponse;
import org.project.domain.label.dto.response.LabelListResponse;
import org.project.domain.label.dto.response.LabelParentListResponse;
import org.project.domain.label.service.LabelService;
import org.project.domain.user.dto.CustomUserDetails;
import org.project.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
