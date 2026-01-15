package org.project.domain.user.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("BlacklistTokenRepository 테스트")
class BlacklistTokenRepositoryTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private BlacklistTokenRepository blacklistTokenRepository;

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("jti와 ttl로 블랙리스트 토큰을 저장한다")
        void save_success() {
            // given
            String jti = "test-jti";
            long ttlSeconds = 3600L;

            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            // when
            blacklistTokenRepository.save(jti, ttlSeconds);

            // then
            verify(valueOperations).set(
                    eq("blacklist-token:" + jti),
                    eq("logout"),
                    eq(Duration.ofSeconds(ttlSeconds))
            );
        }
    }

    @Nested
    @DisplayName("exists")
    class Exists {

        @Test
        @DisplayName("블랙리스트에 존재하면 true를 반환한다")
        void exists_true() {
            // given
            String jti = "exist-jti";
            given(redisTemplate.hasKey("blacklist-token:" + jti)).willReturn(true);

            // when
            boolean result = blacklistTokenRepository.exists(jti);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("블랙리스트에 존재하지 않으면 false를 반환한다")
        void exists_false() {
            // given
            String jti = "not-exist-jti";
            given(redisTemplate.hasKey("blacklist-token:" + jti)).willReturn(false);

            // when
            boolean result = blacklistTokenRepository.exists(jti);

            // then
            assertThat(result).isFalse();
        }
    }
}
