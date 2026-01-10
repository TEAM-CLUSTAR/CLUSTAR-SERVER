package org.project.global.util;

import io.micrometer.common.lang.Nullable;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@Slf4j
public class CookieUtil {

    public static void addCookie(
            HttpServletResponse response,
            String name,
            String value,
            String domain,
            boolean secure,
            String sameSite
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("=").append(value)
                .append("; Path=/")
                .append("; HttpOnly")
                .append("; Max-Age=").append(7 * 24 * 60 * 60); // 7일

        // Domain
        if (StringUtils.hasText(domain)) {
            sb.append("; Domain=").append(domain);
        }

        // Secure
        if (secure) {
            sb.append("; Secure");
        }

        // SameSite
        if (StringUtils.hasText(sameSite)) {
            sb.append("; SameSite=").append(sameSite);
        }

        String cookieHeader = sb.toString();

        response.addHeader("Set-Cookie", cookieHeader);
    }

    public static void deleteCookie(HttpServletResponse response,
                                    String name,
                                    @Nullable String domain,
                                    boolean secure,
                                    @Nullable String sameSite) {

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s=; Path=/; Max-Age=0; HttpOnly", name));   // 값 비우고 즉시 만료

        // Domain
        if (StringUtils.hasText(domain)) {
            sb.append("; Domain=").append(domain);
        }

        // Secure
        if (secure) {                       // prod ⇒ true, local ⇒ false
            sb.append("; Secure");
        }

        // SameSite
        if (StringUtils.hasText(sameSite)) {
            sb.append("; SameSite=").append(sameSite);
        }

        response.addHeader("Set-Cookie", sb.toString());
    }

    public static String getRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals("refreshToken")) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
