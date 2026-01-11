package org.project.global.security.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "spring.security.oauth2.client")
public class GoogleOAuthProperties {

    private Registration registration;
    private Provider provider;

    @Getter
    @Setter
    public static class Registration {
        private Google google;

        @Getter
        @Setter
        public static class Google {
            private String clientId;
            private String clientSecret;
            private String redirectUri;
            private String authorizationGrantType;
            private List<String> scope;
        }
    }

    @Getter
    @Setter
    public static class Provider {
        private Google google;

        @Getter
        @Setter
        public static class Google {
            private String tokenUri;
            private String userInfoUri;
        }
    }
}
