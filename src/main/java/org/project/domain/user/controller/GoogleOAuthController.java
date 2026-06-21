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
import org.project.global.exception.domainException.LoginException;
import org.project.global.exception.errorcode.LoginErrorCode;
import org.project.global.response.ApiResponse;
import org.project.global.security.properties.GoogleOAuthProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class GoogleOAuthController {

    private final GoogleOAuthProperties googleOAuthProperties;
    private final GoogleAuthService googleAuthService;
    @org.springframework.beans.factory.annotation.Value("${app.oauth.frontend-callback-url:http://localhost:5173/oauth/callback}")
    private String frontendCallbackUrl;

    @GetMapping("/oauth/google")
    @Operation(summary = "구글 소셜로그인 API",
            description = "구글로 로그인 요청을 전송합니다.\n" +
                    "그후 아래 API에 인가코드를 넣어 요청합니다.")
    @BusinessExceptionDescription(SwaggerResponseDescription.GOOGLE_LOGIN)
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
            description = "구글 콜백에서 받은 인가코드로 로그인 처리를 한 뒤\n" +
                    "프론트엔드의 콜백 URL로 code만 전달해 리다이렉트합니다.")
    @GetMapping("/oauth/google/callback")
    @BusinessExceptionDescription(SwaggerResponseDescription.GOOGLE_LOGIN_CALLBACK)
    public ResponseEntity<Void> callback(
            @RequestParam Map<String, String> params,
            HttpServletResponse response
    ) {
        String code = params.get("code");

        if (code == null || code.isBlank()) {
            throw new LoginException(LoginErrorCode.AUTH_SOCIAL_LOGIN_FAIL);
        }

        googleAuthService.loginOrRegisterWithResponse(code, response);

        URI redirectUri = UriComponentsBuilder
                .fromUriString(frontendCallbackUrl)
                .queryParam("code", code)
                .build(true)
                .toUri();

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(redirectUri)
                .build();
    }

    @Operation(summary = "로그아웃 API")
    @PostMapping("/logout")
    @BusinessExceptionDescription(SwaggerResponseDescription.LOGOUT)
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
