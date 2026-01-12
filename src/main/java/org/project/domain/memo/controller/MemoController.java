package org.project.domain.memo.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.domain.memo.dto.request.MemoCreateRequest;
import org.project.domain.memo.dto.response.MemoResponse;
import org.project.domain.memo.service.MemoService;
import org.project.domain.user.dto.CustomUserDetails;
import org.project.global.annotation.BusinessExceptionDescription;
import org.project.global.config.swagger.SwaggerResponseDescription;
import org.project.global.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/memo")
@Tag(name = "메모", description = "메모 작성, 검색 등 API")
public class MemoController {

    private final MemoService memoService;

    @Operation(summary = "메모 작성", description = "일반 메모를 작성합니다.")
    @PostMapping
    @BusinessExceptionDescription(SwaggerResponseDescription.CREATE_MEMO)
    public ResponseEntity<ApiResponse<MemoResponse>> createMemo(@AuthenticationPrincipal CustomUserDetails userDetails, @Valid @RequestBody MemoCreateRequest request) {

        Long userId = userDetails.getUserId();

        MemoResponse response = memoService.createMemo(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }


}
