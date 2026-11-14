package org.project.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.project.domain.user.dto.CustomUserDetails;
import org.project.domain.user.service.GoogleAuthService;
import org.project.global.annotation.BusinessExceptionDescription;
import org.project.global.config.swagger.SwaggerResponseDescription;
import org.project.global.exception.domainException.LoginException;
import org.project.global.exception.errorcode.LoginErrorCode;
import org.project.global.response.ApiResponse;
import org.project.global.security.properties.GoogleOAuthProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.WebUtils;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class GoogleOAuthController {

    private static final String GOOGLE_CALLBACK_PATH = "/oauth/google/callback";
    private static final String FRONTEND_CALLBACK_PATH = "/oauth/callback";
    private static final String STATE_COOKIE_PREFIX = "google_oauth_state_";
    private static final Duration STATE_COOKIE_MAX_AGE = Duration.ofMinutes(10);

    private final GoogleOAuthProperties googleOAuthProperties;
    private final GoogleAuthService googleAuthService;

    @Value("${app.oauth.local-frontend-origin}")
    private String localFrontendOrigin;

    @Value("${app.oauth.deployed-frontend-origin}")
    private String deployedFrontendOrigin;

    @GetMapping("/oauth/google")
    @Operation(summary = "구글 소셜로그인 API",
            description = "로그인을 시작한 프론트엔드 오리진을 선택한 뒤 구글로 리다이렉트합니다.")
    @BusinessExceptionDescription(SwaggerResponseDescription.GOOGLE_LOGIN)
    public ResponseEntity<Void> redirectToGoogleAuth(HttpServletRequest request) {
        var google = googleOAuthProperties.getRegistration().getGoogle();
        String state = UUID.randomUUID().toString();
        String frontend = localFrontendOrigin.equals(requestOrigin(request)) ? "local" : "deployed";

        URI authUrl = UriComponentsBuilder.fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", "{clientId}")
                .queryParam("redirect_uri", "{redirectUri}")
                .queryParam("response_type", "code")
                .queryParam("scope", "{scope}")
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .queryParam("state", state)
                .encode()
                .buildAndExpand(google.getClientId(), google.getRedirectUri(), String.join(" ", google.getScope()))
                .toUri();

        return ResponseEntity.status(HttpStatus.FOUND)
                .cacheControl(CacheControl.noStore())
                .location(authUrl)
                .header(HttpHeaders.SET_COOKIE, stateCookie(state, frontend, STATE_COOKIE_MAX_AGE).toString())
                .build();
    }

    @Operation(summary = "구글 인증서버 토큰 검증 API",
            description = "구글 콜백의 state를 확인해 로그인 시작 시 선택한 프론트엔드로 code를 전달합니다.\n" +
                    "프론트엔드가 code를 다시 요청하면 로그인 처리를 진행합니다.")
    @GetMapping(GOOGLE_CALLBACK_PATH)
    @BusinessExceptionDescription(SwaggerResponseDescription.GOOGLE_LOGIN_CALLBACK)
    public ResponseEntity<?> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (code == null || code.isBlank()) {
            throw new LoginException(LoginErrorCode.AUTH_SOCIAL_LOGIN_FAIL);
        }

        if (state == null) {
            String origin = requestOrigin(request);
            if (!localFrontendOrigin.equals(origin) && !deployedFrontendOrigin.equals(origin)) {
                throw new LoginException(LoginErrorCode.AUTH_SOCIAL_LOGIN_FAIL);
            }
            return googleAuthService.loginOrRegisterWithResponse(code, response);
        }

        String frontendOrigin = frontendOriginFromState(request, state);
        URI redirectUri = UriComponentsBuilder
                .fromUriString(frontendOrigin)
                .path(FRONTEND_CALLBACK_PATH)
                .queryParam("code", "{code}")
                .encode()
                .buildAndExpand(code)
                .toUri();

        return ResponseEntity.status(HttpStatus.FOUND)
                .cacheControl(CacheControl.noStore())
                .location(redirectUri)
                .header(HttpHeaders.SET_COOKIE, stateCookie(state, "", Duration.ZERO).toString())
                .build();
    }

    private String frontendOriginFromState(HttpServletRequest request, String state) {
        if (!state.matches("[a-f0-9-]{36}")) {
            throw new LoginException(LoginErrorCode.AUTH_SOCIAL_LOGIN_FAIL);
        }

        Cookie cookie = WebUtils.getCookie(request, STATE_COOKIE_PREFIX + state);
        if (cookie == null) {
            throw new LoginException(LoginErrorCode.AUTH_SOCIAL_LOGIN_FAIL);
        }

        return switch (cookie.getValue()) {
            case "local" -> localFrontendOrigin;
            case "deployed" -> deployedFrontendOrigin;
            default -> throw new LoginException(LoginErrorCode.AUTH_SOCIAL_LOGIN_FAIL);
        };
    }

    private ResponseCookie stateCookie(String state, String value, Duration maxAge) {
        String googleRedirectUri = googleOAuthProperties.getRegistration().getGoogle().getRedirectUri();
        // 탭마다 로그인 요청의 state 쿠키를 따로 유지한다.
        return ResponseCookie.from(STATE_COOKIE_PREFIX + state, value)
                .httpOnly(true)
                .secure("https".equalsIgnoreCase(URI.create(googleRedirectUri).getScheme()))
                .sameSite("Lax")
                .path(GOOGLE_CALLBACK_PATH)
                .maxAge(maxAge)
                .build();
    }

    private String requestOrigin(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin != null) {
            return origin;
        }

        String referer = request.getHeader(HttpHeaders.REFERER);
        if (referer == null) {
            return null;
        }

        try {
            URI uri = URI.create(referer);
            if (uri.getHost() != null && uri.getRawUserInfo() == null) {
                return uri.getScheme() + "://" + uri.getRawAuthority();
            }
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        return null;
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
