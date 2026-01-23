package org.project.global.config.security;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WhiteListConfig {

    // 스웨거 관련 인가 설정
    public static final List<String> swaggerWhitelist() {
        return List.of(
                "/oauth/google/**",  // 구글 로그인
                "/oauth/reissue",  // 토큰 재발급
                "/access",  // 테스트용 엑세스 토큰
                "/actuator/health",
                "/actuator/health/**",
                "/actuator/prometheus"
        );
    }
}
