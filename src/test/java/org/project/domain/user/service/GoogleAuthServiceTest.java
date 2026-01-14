package org.project.domain.user.service;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
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
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoogleAuthService 테스트")
class GoogleAuthServiceTest {
    

    @InjectMocks
    private GoogleAuthService googleAuthService;

    @Mock private GoogleClient googleClient;
    @Mock private UserRepository userRepository;
    @Mock private JWTUtil jwtUtil;
    @Mock private JwtProperties jwtProperties;
    @Mock private CookieConfig cookieConfig;

    @Mock private org.project.global.util.TokenResponseBuilder tokenResponseBuilder;
    @Mock private BlacklistTokenRepository blacklistTokenRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private LabelRepository labelRepository;

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;

    @Mock private CustomUserDetails userDetails;

    private GoogleAccountProfileResponse profile;
    private User user;

    @Nested
    @DisplayName("loginOrRegister")
    class loginOrRegister {

        @BeforeEach
        void setUp() {
            profile = new GoogleAccountProfileResponse(
                    "google-sub",
                    "홍길동",
                    "길동",
                    "홍",
                    "https://image.url",
                    "test@test.com",
                    true,
                    "ko"
            );

            user = User.builder()
                    .id(1L)
                    .email(profile.email())
                    .name(profile.name())
                    .profileImageUrl(profile.picture())
                    .build();
        }

        @Test
        @DisplayName("기존 유저면 회원가입 없이 JWT만 발급된다")
        void login_existing_user_success() {
            // given
            when(googleClient.getGoogleAccountProfile(any()))
                    .thenReturn(profile);

            when(userRepository.findByEmail(profile.email()))
                    .thenReturn(Optional.of(user));

            when(jwtUtil.generateAccessToken(user.getId()))
                    .thenReturn("access-token");

            when(jwtUtil.generateRefreshToken(user.getId()))
                    .thenReturn("refresh-token");

            when(jwtUtil.getJti("refresh-token"))
                    .thenReturn("refresh-jti");

            when(jwtUtil.getRemainingExpiration("refresh-token"))
                    .thenReturn(1000L);

            // when
            JwtLoginResponse response =
                    googleAuthService.loginOrRegister("google-code");

            // then
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");
            assertThat(response.name()).isEqualTo(user.getName());
            assertThat(response.profileImageUrl()).isEqualTo(user.getProfileImageUrl());

            verify(labelRepository, never()).saveAll(any());
            verify(refreshTokenRepository, times(1))
                    .save("refresh-jti", 1000L);
        }

        @Test
        @DisplayName("신규 유저면 회원 생성 + 기본 라벨 생성 + JWT 발급")
        void login_new_user_success() {
            // given
            when(googleClient.getGoogleAccountProfile(any()))
                    .thenReturn(profile);

            when(userRepository.findByEmail(profile.email()))
                    .thenReturn(Optional.empty());

            when(userRepository.save(any(User.class)))
                    .thenReturn(user);

            when(jwtUtil.generateAccessToken(user.getId()))
                    .thenReturn("access-token");

            when(jwtUtil.generateRefreshToken(user.getId()))
                    .thenReturn("refresh-token");

            when(jwtUtil.getJti("refresh-token"))
                    .thenReturn("refresh-jti");

            when(jwtUtil.getRemainingExpiration("refresh-token"))
                    .thenReturn(1000L);

            // when
            JwtLoginResponse response =
                    googleAuthService.loginOrRegister("google-code");

            // then
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo("access-token");

            verify(userRepository, times(1)).save(any(User.class));
            verify(labelRepository, times(1)).saveAll(any(List.class));
            verify(refreshTokenRepository, times(1))
                    .save("refresh-jti", 1000L);
        }

        @Test
        @DisplayName("GoogleClient 예외 발생 시 로그인 실패")
        void loginOrRegister_googleClientFail() {
            // given
            when(googleClient.getGoogleAccountProfile(any()))
                    .thenThrow(new RuntimeException("Google API error"));

            // when & then
            assertThatThrownBy(() -> googleAuthService.loginOrRegister("code"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Google API error");

            verify(googleClient, times(1)).getGoogleAccountProfile(any());
            verifyNoInteractions(userRepository, jwtUtil, refreshTokenRepository);
        }

        @Test
        @DisplayName("JWT 생성 실패 시 로그인 실패")
        void loginOrRegister_jwtGenerationFail() {
            // given
            when(googleClient.getGoogleAccountProfile(any()))
                    .thenReturn(profile);

            when(userRepository.findByEmail(profile.email()))
                    .thenReturn(Optional.of(user));

            when(jwtUtil.generateAccessToken(user.getId()))
                    .thenThrow(new RuntimeException("JWT 생성 실패"));

            // when & then
            assertThatThrownBy(() -> googleAuthService.loginOrRegister("code"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("JWT 생성 실패");

            verify(jwtUtil, times(1)).generateAccessToken(user.getId());
            verify(jwtUtil, never()).generateRefreshToken(any());
            verifyNoInteractions(refreshTokenRepository);
        }

        @Test
        @DisplayName("RefreshToken 저장 실패 시 로그인 실패")
        void loginOrRegister_refreshTokenSaveFail() {
            // given
            when(googleClient.getGoogleAccountProfile(any()))
                    .thenReturn(profile);

            when(userRepository.findByEmail(profile.email()))
                    .thenReturn(Optional.of(user));

            when(jwtUtil.generateAccessToken(user.getId()))
                    .thenReturn("access-token");

            when(jwtUtil.generateRefreshToken(user.getId()))
                    .thenReturn("refresh-token");

            when(jwtUtil.getJti("refresh-token"))
                    .thenReturn("jti");

            when(jwtUtil.getRemainingExpiration("refresh-token"))
                    .thenReturn(3600L);

            doThrow(new RuntimeException("Redis 저장 실패"))
                    .when(refreshTokenRepository)
                    .save(any(), anyLong());

            // when & then
            assertThatThrownBy(() -> googleAuthService.loginOrRegister("code"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Redis 저장 실패");

            verify(refreshTokenRepository, times(1))
                    .save("jti", 3600L);
        }
    }

    @Nested
    @DisplayName("logout")
    class logout {

        @Mock
        private User user;

        @BeforeEach
        void setUp() {
            when(userDetails.getUser()).thenReturn(user);
            when(user.getId()).thenReturn(1L);

            when(jwtProperties.getHeader()).thenReturn("Authorization");
            when(cookieConfig.getDomain()).thenReturn("localhost");
            when(cookieConfig.isSecure()).thenReturn(false);
            when(cookieConfig.getSameSite()).thenReturn("Lax");
        }

        @Test
        @DisplayName("Refresh Token이 존재하면 화이트리스트에서 삭제한다")
        void delete_refresh_token_success() {
            // given
            String refreshToken = "refresh-token";
            try (MockedStatic<CookieUtil> cookieUtil = mockStatic(CookieUtil.class)) {

                cookieUtil.when(() -> CookieUtil.getRefreshTokenFromCookie(request))
                        .thenReturn(refreshToken);

                when(jwtUtil.getJti(refreshToken)).thenReturn("refresh-jti");

                // when
                googleAuthService.logout(userDetails, request, response);

                // then
                verify(refreshTokenRepository, times(1))
                        .delete("refresh-jti");
            }
        }

        @Test
        @DisplayName("Refresh Token이 없으면 삭제를 시도하지 않는다")
        void refresh_token_not_exists() {
            try (MockedStatic<CookieUtil> cookieUtil = mockStatic(CookieUtil.class)) {

                cookieUtil.when(() -> CookieUtil.getRefreshTokenFromCookie(request))
                        .thenReturn(null);

                googleAuthService.logout(userDetails, request, response);

                verifyNoInteractions(refreshTokenRepository);
            }
        }

        @Test
        @DisplayName("Authorization 헤더가 있으면 블랙리스트에 등록한다")
        void blacklist_access_token_success() {
            // given
            when(request.getHeader("Authorization"))
                    .thenReturn("Bearer access-token");

            when(jwtUtil.getJti("access-token"))
                    .thenReturn("access-jti");

            when(jwtUtil.getRemainingExpiration("access-token"))
                    .thenReturn(3600L);

            // when
            googleAuthService.logout(userDetails, request, response);

            // then
            verify(blacklistTokenRepository, times(1))
                    .save("access-jti", 3600L);
        }

        @Test
        @DisplayName("Authorization 헤더가 없으면 블랙리스트 등록하지 않는다")
        void no_authorization_header() {
            when(request.getHeader("Authorization")).thenReturn(null);

            googleAuthService.logout(userDetails, request, response);

            verifyNoInteractions(blacklistTokenRepository);
        }

        @Test
        @DisplayName("Refresh Token 삭제 중 예외 발생해도 로그아웃은 계속 진행된다")
        void refresh_token_exception_handled() {
            String refreshToken = "refresh-token";

            try (MockedStatic<CookieUtil> cookieUtil = mockStatic(CookieUtil.class)) {

                cookieUtil.when(() -> CookieUtil.getRefreshTokenFromCookie(request))
                        .thenReturn(refreshToken);

                when(jwtUtil.getJti(refreshToken))
                        .thenThrow(new RuntimeException("JWT 파싱 실패"));

                assertDoesNotThrow(() ->
                        googleAuthService.logout(userDetails, request, response)
                );
            }
        }

        @Test
        @DisplayName("Access Token 블랙리스트 등록 실패해도 예외를 던지지 않는다")
        void access_token_exception_handled() {
            when(request.getHeader("Authorization"))
                    .thenReturn("Bearer access-token");

            when(jwtUtil.getJti("access-token"))
                    .thenThrow(new RuntimeException("JWT 파싱 실패"));

            assertDoesNotThrow(() ->
                    googleAuthService.logout(userDetails, request, response)
            );
        }

        @Test
        @DisplayName("로그아웃 시 항상 Refresh Token 쿠키를 삭제한다")
        void delete_cookie_always() {
            try (MockedStatic<CookieUtil> cookieUtil = mockStatic(CookieUtil.class)) {

                googleAuthService.logout(userDetails, request, response);

                cookieUtil.verify(() ->
                                CookieUtil.deleteCookie(
                                        response,
                                        "refreshToken",
                                        "localhost",
                                        false,
                                        "Lax"
                                ),
                        times(1)
                );
            }
        }
    }

    @Nested
    @DisplayName("reissueToken")
    class ReissueToken {

        @BeforeEach
        void setUp() {
            user = User.builder()
                    .id(1L)
                    .email("test@test.com")
                    .name("tester")
                    .build();
        }

        @Test
        @DisplayName("정상적으로 Access / Refresh Token을 재발급한다")
        void reissue_success() {
            // given
            String oldRefreshToken = "old-refresh-token";
            String oldJti = "old-jti";
            String newAccessToken = "new-access-token";
            String newRefreshToken = "new-refresh-token";
            String newJti = "new-jti";

            try (MockedStatic<CookieUtil> cookieUtil = mockStatic(CookieUtil.class)) {

                cookieUtil.when(() ->
                        CookieUtil.getRefreshTokenFromCookie(request)
                ).thenReturn(oldRefreshToken);

                when(jwtUtil.validateRefreshToken(oldRefreshToken))
                        .thenReturn(1L);

                when(jwtUtil.getJti(oldRefreshToken))
                        .thenReturn(oldJti);

                when(refreshTokenRepository.exists(oldJti))
                        .thenReturn(true);

                when(userRepository.findById(1L))
                        .thenReturn(Optional.of(user));

                when(jwtUtil.generateAccessToken(1L))
                        .thenReturn(newAccessToken);

                when(jwtUtil.generateRefreshToken(1L))
                        .thenReturn(newRefreshToken);

                when(jwtUtil.getJti(newRefreshToken))
                        .thenReturn(newJti);

                when(jwtUtil.getRemainingExpiration(newRefreshToken))
                        .thenReturn(3600L);

                ResponseEntity<ApiResponse<Void>> responseEntity =
                        ResponseEntity.ok(ApiResponse.ok(null));

                when(tokenResponseBuilder.buildTokenResponse(
                        newAccessToken,
                        newRefreshToken,
                        response
                )).thenReturn(responseEntity);

                // when
                ResponseEntity<ApiResponse<Void>> result =
                        googleAuthService.reissueToken(request, response);

                // then
                assertThat(result).isNotNull();

                verify(refreshTokenRepository).delete(oldJti);
                verify(refreshTokenRepository).save(newJti, 3600L);
            }
        }

        @Test
        @DisplayName("Refresh Token이 없으면 예외 발생")
        void refresh_token_not_found() {
            try (MockedStatic<CookieUtil> cookieUtil = mockStatic(CookieUtil.class)) {

                cookieUtil.when(() ->
                        CookieUtil.getRefreshTokenFromCookie(request)
                ).thenReturn(null);

                assertThatThrownBy(() ->
                        googleAuthService.reissueToken(request, response)
                )
                        .isInstanceOf(LoginException.class)
                        .hasMessageContaining(LoginErrorCode.REFRESH_TOKEN_NOT_FOUND.getMsg());
            }
        }

        @Test
        @DisplayName("화이트리스트에 없으면 INVALID_REFRESH_TOKEN")
        void refresh_token_not_in_whitelist() {
            String refreshToken = "refresh-token";

            try (MockedStatic<CookieUtil> cookieUtil = mockStatic(CookieUtil.class)) {

                cookieUtil.when(() ->
                        CookieUtil.getRefreshTokenFromCookie(request)
                ).thenReturn(refreshToken);

                when(jwtUtil.validateRefreshToken(refreshToken))
                        .thenReturn(1L);

                when(jwtUtil.getJti(refreshToken))
                        .thenReturn("jti");

                when(refreshTokenRepository.exists("jti"))
                        .thenReturn(false);

                assertThatThrownBy(() ->
                        googleAuthService.reissueToken(request, response)
                )
                        .isInstanceOf(LoginException.class)
                        .hasMessageContaining(LoginErrorCode.INVALID_REFRESH_TOKEN.getMsg());
            }
        }

        @Test
        @DisplayName("User가 존재하지 않으면 예외 발생")
        void user_not_found() {
            String refreshToken = "refresh-token";

            try (MockedStatic<CookieUtil> cookieUtil = mockStatic(CookieUtil.class)) {

                cookieUtil.when(() ->
                        CookieUtil.getRefreshTokenFromCookie(request)
                ).thenReturn(refreshToken);

                when(jwtUtil.validateRefreshToken(refreshToken))
                        .thenReturn(1L);

                when(jwtUtil.getJti(refreshToken))
                        .thenReturn("jti");

                when(refreshTokenRepository.exists("jti"))
                        .thenReturn(true);

                when(userRepository.findById(1L))
                        .thenReturn(Optional.empty());

                assertThatThrownBy(() ->
                        googleAuthService.reissueToken(request, response)
                )
                        .isInstanceOf(UserException.class)
                        .hasMessageContaining(UserErrorCode.NOT_FOUND_USER.getMsg());
            }
        }

        @Test
        @DisplayName("JWT 검증 실패 시 INVALID_REFRESH_TOKEN")
        void jwt_validation_failed() {
            String refreshToken = "refresh-token";

            try (MockedStatic<CookieUtil> cookieUtil = mockStatic(CookieUtil.class)) {

                cookieUtil.when(() ->
                        CookieUtil.getRefreshTokenFromCookie(request)
                ).thenReturn(refreshToken);

                when(jwtUtil.validateRefreshToken(refreshToken))
                        .thenThrow(new JwtException("invalid"));

                assertThatThrownBy(() ->
                        googleAuthService.reissueToken(request, response)
                )
                        .isInstanceOf(LoginException.class)
                        .hasMessageContaining(LoginErrorCode.INVALID_REFRESH_TOKEN.getMsg());
            }
        }
    }
}
