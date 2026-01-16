package org.project.domain.user.service;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.domain.label.entity.Label;
import org.project.domain.label.repository.LabelRepository;
import org.project.domain.user.dto.CustomUserDetails;
import org.project.domain.user.dto.response.JwtLoginResponse;
import org.project.domain.user.entity.User;
import org.project.domain.user.repository.BlacklistTokenRepository;
import org.project.domain.user.repository.RefreshTokenRepository;
import org.project.domain.user.repository.UserRepository;
import org.project.global.config.security.CookieConfig;
import org.project.global.exception.domainException.LoginException;
import org.project.global.exception.domainException.UserException;
import org.project.global.exception.errorcode.LoginErrorCode;
import org.project.global.exception.errorcode.UserErrorCode;
import org.project.global.response.ApiResponse;
import org.project.global.security.client.GoogleClient;
import org.project.global.security.client.dto.GoogleAccountProfileResponse;
import org.project.global.security.properties.JwtProperties;
import org.project.global.util.CookieUtil;
import org.project.global.util.JWTUtil;
import org.project.global.util.TokenResponseBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final GoogleClient googleClient;
    private final UserRepository userRepository;
    private final JWTUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final CookieConfig cookieConfig;

    private final TokenResponseBuilder tokenResponseBuilder;
    private final BlacklistTokenRepository blacklistTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LabelRepository labelRepository;

    public ResponseEntity<ApiResponse<Void>> loginOrRegisterWithResponse(String code,
                                                                         HttpServletResponse response) {
        try {
            JwtLoginResponse jwtLoginResponse = loginOrRegister(code);
            return tokenResponseBuilder.buildTokenResponse(jwtLoginResponse.accessToken(), jwtLoginResponse.refreshToken(), response);
        }
        catch (Exception e) {
            log.error("소셜 로그인 실패: {}", e.getMessage(), e);
            throw new LoginException(LoginErrorCode.AUTH_SOCIAL_LOGIN_FAIL);
        }
    }

    @Transactional
    public JwtLoginResponse loginOrRegister(String code) {

        // 1. Google 유저정보 가져오기
        GoogleAccountProfileResponse profile = googleClient.getGoogleAccountProfile(code);

        // 2. DB에 유저 존재 여부 확인
        User user;
        boolean isNewUser = false;

        Optional<User> optionalUser = userRepository.findByEmail(profile.email());

        if (optionalUser.isPresent()) {
            user = optionalUser.get();
        } else {
            user = userRepository.save(
                    User.createSocialUser(profile.email(), profile.name(), profile.picture(), "google")
            );
            isNewUser = true;
        }

        if (isNewUser) {
            labelRepository.saveAll(Label.createDefaultLabels(user));
            log.info("기본 라벨 생성 완료 - userId: {}", user.getId());
        }

        // 3. JWT 토큰 생성
        String serverAccessToken = jwtUtil.generateAccessToken(user.getId());
        String serverRefreshToken = jwtUtil.generateRefreshToken(user.getId());

        // 4. 리프레시 토큰 화이트리스트 등록
        String refreshJti = jwtUtil.getJti(serverRefreshToken);
        long refreshTtl = jwtUtil.getRemainingExpiration(serverRefreshToken);

        refreshTokenRepository.save(refreshJti, refreshTtl);

        log.info("JWT 발급 완료 - userId: {}", user.getId());

        return JwtLoginResponse.of(user, serverAccessToken, serverRefreshToken);
    }

    public void logout(CustomUserDetails userDetails, HttpServletRequest request, HttpServletResponse response) {
        Long id = userDetails.getUser().getId();

        // 1. Refresh Token 무효화 (화이트리스트 제거)
        String refreshToken = CookieUtil.getRefreshTokenFromCookie(request);

        if (refreshToken != null) {
            try {
                String refreshJti = jwtUtil.getJti(refreshToken);
                refreshTokenRepository.delete(refreshJti);
            } catch (Exception e) {
                log.warn("Refresh Token 삭제 실패: {}", e.getMessage());
//                throw new LoginException(LoginErrorCode.REFRESH_TOKEN_DELETE_FAILED);
            }
        }

        // 2. Access Token 블랙리스트 등록
        String authorizationHeader = request.getHeader(jwtProperties.getHeader());

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            try {
                String accessToken = authorizationHeader.substring(7).trim();
                String accessJti = jwtUtil.getJti(accessToken);
                long accessTtl = jwtUtil.getRemainingExpiration(accessToken);
                blacklistTokenRepository.save(accessJti, accessTtl);
            } catch (Exception e) {
                log.warn("액세스 토큰 블랙리스트 등록 실패: {}", e.getMessage());
//                throw new LoginException(LoginErrorCode.ACCESS_TOKEN_BLACKLIST_FAILED);
            }
        }

        // 3. 쿠키 삭제
        CookieUtil.deleteCookie(response, "refreshToken",
                cookieConfig.getDomain(), cookieConfig.isSecure(), cookieConfig.getSameSite());
    }

    public ResponseEntity<ApiResponse<Void>> reissueToken(HttpServletRequest request, HttpServletResponse response) {
        // 쿠키에서 Refresh Token 가져오기
        String refreshToken = CookieUtil.getRefreshTokenFromCookie(request);

        if (refreshToken == null) {
            throw new LoginException(LoginErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        try {
            Long userId = jwtUtil.validateRefreshToken(refreshToken);
            String oldJti = jwtUtil.getJti(refreshToken);

            // 화이트리스트에 없으면 재사용 공격
            if (!refreshTokenRepository.exists(oldJti)) {
                throw new LoginException(LoginErrorCode.INVALID_REFRESH_TOKEN);
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND_USER));

            // 새 토큰 발급
            String newAccessToken = jwtUtil.generateAccessToken(user.getId());
            String newRefreshToken = jwtUtil.generateRefreshToken(user.getId());
            String newJti = jwtUtil.getJti(newRefreshToken);

            // 기존 Refresh Token 제거
            refreshTokenRepository.delete(oldJti);

            // 새 Refresh Token 저장 (화이트리스트)
            long ttl = jwtUtil.getRemainingExpiration(newRefreshToken);
            refreshTokenRepository.save(newJti, ttl);

            return tokenResponseBuilder.buildTokenResponse(
                    newAccessToken,
                    newRefreshToken,
                    response
            );
        } catch (JwtException e) {
            throw new LoginException(LoginErrorCode.INVALID_REFRESH_TOKEN);
        }
    }
}
