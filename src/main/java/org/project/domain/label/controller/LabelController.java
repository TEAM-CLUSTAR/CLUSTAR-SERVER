package org.project.domain.label.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.project.domain.label.dto.reponse.LabelListResponse;
import org.project.domain.label.service.LabelService;
import org.project.domain.user.dto.CustomUserDetails;
import org.project.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
