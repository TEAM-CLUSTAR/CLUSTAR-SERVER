package org.project.global.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class CorsLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String origin = request.getHeader("Origin");
        String method = request.getMethod();

        // CORS Preflight 요청 로깅
        if ("OPTIONS".equalsIgnoreCase(method)) {
            log.warn("[CORS PRE-FLIGHT] method={}, origin={}, requestURL={}",
                    method, origin, request.getRequestURL());
        }

        // CORS 에러 로그 추가
        response.addHeader("Access-Control-Expose-Headers", "X-Cors-Error");

        filterChain.doFilter(request, response);
    }
}
