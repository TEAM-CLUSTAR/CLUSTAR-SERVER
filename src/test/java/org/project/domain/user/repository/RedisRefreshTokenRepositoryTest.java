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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisRefreshTokenRepository 테스트")
class RedisRefreshTokenRepositoryTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisRefreshTokenRepository redisRefreshTokenRepository;

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("jti와 ttl로 refresh 토큰을 저장한다")
        void save_success() {
            // given
            String jti = "refresh-jti";
            long ttlSeconds = 7200L;

            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            // when
            redisRefreshTokenRepository.save(jti, ttlSeconds);

            // then
            verify(valueOperations).set(
                    eq("refresh:" + jti),
                    eq("valid"),
                    eq(Duration.ofSeconds(ttlSeconds))
            );
        }
    }

    @Nested
    @DisplayName("exists")
    class Exists {

        @Test
        @DisplayName("refresh 토큰이 존재하면 true를 반환한다")
        void exists_true() {
            // given
            String jti = "exist-jti";
            given(redisTemplate.hasKey("refresh:" + jti)).willReturn(true);

            // when
            boolean result = redisRefreshTokenRepository.exists(jti);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("refresh 토큰이 존재하지 않으면 false를 반환한다")
        void exists_false() {
            // given
            String jti = "not-exist-jti";
            given(redisTemplate.hasKey("refresh:" + jti)).willReturn(false);

            // when
            boolean result = redisRefreshTokenRepository.exists(jti);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("refresh 토큰을 삭제한다")
        void delete_success() {
            // given
            String jti = "delete-jti";

            // when
            redisRefreshTokenRepository.delete(jti);

            // then
            verify(redisTemplate).delete("refresh:" + jti);
        }
    }
}
