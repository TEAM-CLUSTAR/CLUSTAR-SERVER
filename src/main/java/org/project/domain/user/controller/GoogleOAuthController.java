package org.project.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.domain.user.dto.CustomUserDetails;
import org.project.domain.user.service.GoogleAuthService;
import org.project.global.annotation.BusinessExceptionDescription;
import org.project.global.config.swagger.SwaggerResponseDescription;
import org.project.global.response.ApiResponse;
import org.project.global.security.properties.GoogleOAuthProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class GoogleOAuthController {

    private final GoogleOAuthProperties googleOAuthProperties;
    private final GoogleAuthService googleAuthService;

    @GetMapping("/oauth/google")
    @Operation(summary = "구글 소셜로그인 API",
            description = "구글로 로그인 요청을 전송합니다.\n" +
                    "그후 아래 API에 인가코드를 넣어 요청합니다.")
    public ResponseEntity<Void> redirectToGoogleAuth() {
        String redirectUri = googleOAuthProperties.getRegistration().getGoogle().getRedirectUri();
        String clientId = googleOAuthProperties.getRegistration().getGoogle().getClientId();
        String scope = String.join(" ", googleOAuthProperties.getRegistration().getGoogle().getScope());

        String authUrl = "https://accounts.google.com/o/oauth2/v2/auth" +
                "?client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=" + scope +
                "&access_type=offline" +
                "&prompt=consent";

        return ResponseEntity.status(HttpStatus.FOUND)  // 302
                .header("Location", authUrl)
                .build();
    }

    @Operation(summary = "구글 인증서버 토큰 검증 API",
            description = "리다이렉트에서 AccessCode를 가지고 서버로 돌아오기 위한 엔드포인트입니다\n" +
                    "해당 코드를 이용해서 사용자 정보를 파싱하고 액세스 토큰는 헤더에, 리프레시 토큰은 쿠키에 담아 반환합니다")
    @GetMapping("/oauth/google/callback")
    public ResponseEntity<ApiResponse<Void>> callback(
            @RequestParam String code,
            HttpServletResponse response
    ) {
        return googleAuthService.loginOrRegisterWithResponse(code, response);
    }

    @Operation(summary = "로그아웃 API")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                      HttpServletRequest request,
                                                      HttpServletResponse response) {

        googleAuthService.logout(userDetails, request, response);

        return ResponseEntity.ok(ApiResponse.ok("로그아웃이 정상적으로 처리되었습니다"));
    }


    @Operation(summary = "Access Token 재발급 API",
            description = "Refresh Token을 이용하여 새로운 Access Token을 발급합니다.\n" +
                    "Refresh Token은 Cookie에서 자동으로 읽어옵니다.")
    @PostMapping("/oauth/reissue")
    @BusinessExceptionDescription(SwaggerResponseDescription.REISSUE_TOKEN)
    public ResponseEntity<ApiResponse<Void>> reissueToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return googleAuthService.reissueToken(request, response);
    }
}
