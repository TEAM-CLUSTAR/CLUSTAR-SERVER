package org.project.global.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
@Profile("!test")
public class SwaggerSecurityConfig {

    @Value("${swagger.auth.username:swagger}")
    private String swaggerUsername;

    @Value("${swagger.auth.password}")
    private String swaggerPassword;

    @Bean
    public UserDetailsService swaggerUserDetailsService(PasswordEncoder passwordEncoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername(swaggerUsername)
                        .password(passwordEncoder.encode(swaggerPassword))
                        .roles("SWAGGER")
                        .build()
        );
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
