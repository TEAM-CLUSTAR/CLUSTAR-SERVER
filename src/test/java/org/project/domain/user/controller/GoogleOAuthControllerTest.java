package org.project.domain.user.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.project.domain.user.service.GoogleAuthService;
import org.project.global.response.ApiResponse;
import org.project.global.security.filter.JWTFilter;
import org.project.global.security.properties.GoogleOAuthProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GoogleOAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("GoogleOAuthController 테스트")
class GoogleOAuthControllerTest {

    private static final String GOOGLE_CALLBACK_PATH = "/oauth/google/callback";
    private static final String GOOGLE_REDIRECT_URI = "https://api.example.test" + GOOGLE_CALLBACK_PATH;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GoogleOAuthProperties googleOAuthProperties;

    @MockBean
    private GoogleAuthService googleAuthService;

    @MockBean
    private JWTFilter jwtFilter;

    @BeforeEach
    void setUp() {
        var registration = new GoogleOAuthProperties.Registration();
        var google = new GoogleOAuthProperties.Registration.Google();
        google.setClientId("test-client");
        google.setRedirectUri(GOOGLE_REDIRECT_URI);
        google.setScope(List.of("profile", "email"));
        registration.setGoogle(google);
        when(googleOAuthProperties.getRegistration()).thenReturn(registration);
    }

    @ParameterizedTest
    @CsvSource({
            "Origin, http://localhost:5173, http://localhost:5173",
            "Origin, https://www.clustar.cloud, https://www.clustar.cloud",
            "Referer, http://localhost:5173/login, http://localhost:5173",
            "Referer, https://www.clustar.cloud/login, https://www.clustar.cloud",
            "Referer, http://localhost:5173.evil.com/login, https://www.clustar.cloud",
            "Referer, :invalid, https://www.clustar.cloud"
    })
    @DisplayName("로그인 시작 요청에서 선택한 프론트로 Google 콜백의 code만 전달한다")
    void callback_redirects_to_frontend_selected_at_login(String headerName, String headerValue,
                                                         String frontendOrigin) throws Exception {
        MvcResult login = mockMvc.perform(get("/oauth/google").header(headerName, headerValue))
                .andExpect(status().isFound())
                .andReturn();
        String state = authorizationState(login);
        Cookie stateCookie = stateCookie(login, state);
        assertThat(stateCookie.isHttpOnly()).isTrue();
        assertThat(stateCookie.getSecure()).isTrue();
        assertThat(stateCookie.getPath()).isEqualTo(GOOGLE_CALLBACK_PATH);
        assertThat(stateCookie.getMaxAge()).isEqualTo(600);
        assertThat(login.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("SameSite=Lax");

        MvcResult callback = mockMvc.perform(get(GOOGLE_CALLBACK_PATH)
                        .queryParam("iss", "https://accounts.google.com")
                        .queryParam("code", "4/abc+123&=")
                        .queryParam("state", state)
                        .cookie(stateCookie))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, frontendOrigin + "/oauth/callback?code=4%2Fabc%2B123%26%3D"))
                .andReturn();

        assertThat(stateCookie(callback, state).getMaxAge()).isZero();
        verifyNoInteractions(googleAuthService);
    }

    @Test
    @DisplayName("로그인 시작 오리진을 확인할 수 없으면 배포 프론트로 돌아간다")
    void callback_defaults_to_deployed_frontend() throws Exception {
        MvcResult login = mockMvc.perform(get("/oauth/google"))
                .andExpect(status().isFound())
                .andReturn();
        String state = authorizationState(login);

        mockMvc.perform(get(GOOGLE_CALLBACK_PATH)
                        .queryParam("code", "abc123")
                        .queryParam("state", state)
                        .cookie(stateCookie(login, state)))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://www.clustar.cloud/oauth/callback?code=abc123"));
    }

    @ParameterizedTest
    @CsvSource({
            "Origin, http://localhost:5173",
            "Origin, https://www.clustar.cloud",
            "Referer, https://www.clustar.cloud/oauth/callback"
    })
    @DisplayName("허용된 프론트에서 전달한 code는 백엔드 로그인 처리에 사용한다")
    void callback_processes_frontend_login_request(String headerName, String headerValue) throws Exception {
        when(googleAuthService.loginOrRegisterWithResponse(eq("abc123"), any(HttpServletResponse.class)))
                .thenReturn(ResponseEntity.ok(ApiResponse.ok(null)));

        mockMvc.perform(get(GOOGLE_CALLBACK_PATH)
                        .queryParam("code", "abc123")
                        .header(headerName, headerValue))
                .andExpect(status().isOk());

        verify(googleAuthService).loginOrRegisterWithResponse(eq("abc123"), any(HttpServletResponse.class));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"http://localhost:5173.evil.com/oauth/callback", ":invalid"})
    @DisplayName("state 없는 요청은 허용된 프론트 오리진에서만 처리한다")
    void callback_rejects_unrecognized_frontend_without_state(String referer) throws Exception {
        var request = get(GOOGLE_CALLBACK_PATH).queryParam("code", "abc123");
        if (referer != null) {
            request.header(HttpHeaders.REFERER, referer);
        }

        mockMvc.perform(request).andExpect(status().isUnauthorized());

        verifyNoInteractions(googleAuthService);
    }

    @Test
    @DisplayName("브라우저 쿠키와 다른 OAuth state는 프론트 오리진이 있어도 거부한다")
    void callback_rejects_mismatched_state() throws Exception {
        MvcResult login = mockMvc.perform(get("/oauth/google").header(HttpHeaders.ORIGIN, "http://localhost:5173"))
                .andExpect(status().isFound())
                .andReturn();
        String state = authorizationState(login);

        mockMvc.perform(get(GOOGLE_CALLBACK_PATH)
                        .queryParam("code", "abc123")
                        .queryParam("state", "00000000-0000-0000-0000-000000000000")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .cookie(stateCookie(login, state)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(googleAuthService);
    }

    private String authorizationState(MvcResult result) {
        String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
        assertThat(location).startsWith("https://accounts.google.com/o/oauth2/v2/auth?")
                .contains("redirect_uri=https%3A%2F%2Fapi.example.test%2Foauth%2Fgoogle%2Fcallback");
        String state = UriComponentsBuilder.fromUriString(location).build().getQueryParams().getFirst("state");
        assertThat(state).isNotBlank();
        return state;
    }

    private Cookie stateCookie(MvcResult result, String state) {
        Cookie cookie = result.getResponse().getCookie("google_oauth_state_" + state);
        assertThat(cookie).isNotNull();
        return cookie;
    }
}
