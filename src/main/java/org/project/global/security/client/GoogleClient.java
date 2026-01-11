package org.project.global.security.client;

import lombok.RequiredArgsConstructor;
import org.project.global.exception.domainException.LoginException;
import org.project.global.exception.errorcode.LoginErrorCode;
import org.project.global.security.client.dto.GoogleAccessTokenRequest;
import org.project.global.security.client.dto.GoogleAccessTokenResponse;
import org.project.global.security.client.dto.GoogleAccountProfileResponse;
import org.project.global.security.properties.GoogleOAuthProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GoogleClient {

    private final RestTemplate restTemplate;
    private final GoogleOAuthProperties googleOAuthProperties;

    public GoogleAccountProfileResponse getGoogleAccountProfile(final String code) {
        final String accessToken = requestGoogleAccessToken(code);
        return requestGoogleAccountProfile(accessToken);
    }

    private String requestGoogleAccessToken(final String code) {
        // 인가 코드값 디코딩
        final String decodedCode = URLDecoder.decode(code, StandardCharsets.UTF_8);

        // OAuth 설정 불러오기
        final var registration = googleOAuthProperties.getRegistration().getGoogle();
        final var provider = googleOAuthProperties.getProvider().getGoogle();

        // 구글에 보낼 요청 명세
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        final HttpEntity<GoogleAccessTokenRequest> httpEntity = new HttpEntity<>(
                new GoogleAccessTokenRequest(
                        decodedCode,
                        registration.getClientId(),
                        registration.getClientSecret(),
                        registration.getRedirectUri(),
                        registration.getAuthorizationGrantType()
                ),
                headers
        );

        // 구글 서버에 접근할 엑세스 토큰 받기
        final GoogleAccessTokenResponse response = restTemplate.exchange(
                provider.getTokenUri(), HttpMethod.POST, httpEntity, GoogleAccessTokenResponse.class
        ).getBody();

        return Optional.ofNullable(response)
                .orElseThrow(() -> new LoginException(LoginErrorCode.NOT_FOUND_GOOGLE_ACCESS_TOKEN_RESPONSE))
                .accessToken();
    }

    // 받은 엑세스 토큰으로 유저의 계정 정보 조회
    private GoogleAccountProfileResponse requestGoogleAccountProfile(final String accessToken) {
        final var provider = googleOAuthProperties.getProvider().getGoogle();

        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

        final HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        return restTemplate.exchange(
                provider.getUserInfoUri(),
                HttpMethod.GET,
                httpEntity,
                GoogleAccountProfileResponse.class
        ).getBody();
    }
}
