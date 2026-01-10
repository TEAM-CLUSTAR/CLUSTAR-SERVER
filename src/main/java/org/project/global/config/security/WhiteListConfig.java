package org.project.global.config.security;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WhiteListConfig {

    // 스웨거 관련 인가 설정
    public static final List<String> swaggerWhitelist() {
        return List.of(
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/actuator/health",
                "/actuator/health/**"
        );
    }
}
