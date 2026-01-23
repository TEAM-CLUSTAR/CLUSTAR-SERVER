package org.project.global.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class SwaggerBasicAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws IOException, ServletException {

        String path = request.getRequestURI();

        if (path.startsWith("/swagger-ui")
                || path.startsWith("/swagger-resources")
                || path.startsWith("/v3/api-docs")) {

            if (request.getHeader("Authorization") == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setHeader(
                        "WWW-Authenticate",
                        "Basic realm=\"Swagger UI\""
                );
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
