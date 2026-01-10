package org.project.global.util;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.project.global.config.security.CookieConfig;
import org.project.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenResponseBuilder {

    private final CookieConfig cookieConfig;

    public ResponseEntity<ApiResponse<Void>> buildTokenResponse(
            String accessToken,
            String refreshToken,
            HttpServletResponse response
    ) {
        // refreshToken 쿠키 재설정
        CookieUtil.addCookie(
                response,
                "refreshToken",
                refreshToken,
                cookieConfig.getDomain(),
                cookieConfig.isSecure(),
                cookieConfig.getSameSite()
        );

        // AccessToken 헤더 전달
        response.setHeader("Authorization", "Bearer " + accessToken);

        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
