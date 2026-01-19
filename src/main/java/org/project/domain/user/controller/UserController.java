package org.project.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.project.domain.user.dto.CustomUserDetails;
import org.project.domain.user.dto.response.UserInfoResponse;
import org.project.domain.user.entity.User;
import org.project.domain.user.service.UserService;
import org.project.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
@Tag(name = "유저 관련 API")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(
            summary = "유저 정보 조회",
            description = "유저 정보를 조회하여, 이름, 이메일, 프로필이미지를 올립니다."
    )
    public ResponseEntity<ApiResponse<UserInfoResponse>> getUserInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();

        UserInfoResponse response = userService.getUserInfo(userId);

        return ResponseEntity.ok(
                ApiResponse.ok(response)
        );
    }
}
