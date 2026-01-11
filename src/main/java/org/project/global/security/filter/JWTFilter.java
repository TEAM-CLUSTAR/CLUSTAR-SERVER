package org.project.global.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.domain.user.dto.CustomUserDetails;
import org.project.domain.user.entity.User;
import org.project.domain.user.repository.BlacklistTokenRepository;
import org.project.domain.user.repository.UserRepository;
import org.project.global.exception.BusinessException;
import org.project.global.exception.errorcode.LoginErrorCode;
import org.project.global.security.properties.JwtProperties;
import org.project.global.util.JWTUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final UserRepository userRepository;
    private final BlacklistTokenRepository blacklistTokenRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader(jwtProperties.getHeader()); // ex. "Authorization"

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7); // "Bearer " 이후 토큰만 추출

        //  1. 블랙리스트 확인 (로그아웃된 토큰인지)
        String jti = jwtUtil.getJti(token);
        if (blacklistTokenRepository.exists(jti)) {
            throw new BusinessException(LoginErrorCode.ALREADY_LOGOUT_TOKEN);
        }

        //  2. 유효한 토큰인지 검증
        if (!jwtUtil.isTokenValid(token)) {
            log.warn("Invalid JWT Token");
            throw new BusinessException(LoginErrorCode.INVALID_ACCESS_TOKEN);
        }

        //  3. 유저 정보 세팅
        Long userId = jwtUtil.getUserId(token);
        if (userId == null || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        User user = userOptional.get();
        CustomUserDetails userDetails = new CustomUserDetails(user);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
