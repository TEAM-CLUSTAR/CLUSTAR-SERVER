package org.project.domain.user.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.domain.user.service.GoogleAuthService;
import org.project.global.response.ApiResponse;
import org.project.global.security.filter.JWTFilter;
import org.project.global.security.properties.GoogleOAuthProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GoogleOAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("GoogleOAuthController 테스트")
class GoogleOAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GoogleOAuthProperties googleOAuthProperties;

    @MockBean
    private GoogleAuthService googleAuthService;

    @MockBean
    private JWTFilter jwtFilter;

    @Test
    @DisplayName("Google 콜백은 code만 프론트 callback으로 전달한다")
    void callback_redirects_only_code() throws Exception {
        when(googleAuthService.loginOrRegisterWithResponse(eq("abc123"), any(HttpServletResponse.class)))
                .thenReturn(ResponseEntity.ok(ApiResponse.ok(null)));

        mockMvc.perform(get("/oauth/google/callback")
                        .queryParam("iss", "https://accounts.google.com")
                        .queryParam("code", "abc123"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "http://localhost:5173/oauth/callback?code=abc123"));

        verify(googleAuthService).loginOrRegisterWithResponse(eq("abc123"), any(HttpServletResponse.class));
    }
}
