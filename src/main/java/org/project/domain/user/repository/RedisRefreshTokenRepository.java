package org.project.domain.user.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class RedisRefreshTokenRepository implements RefreshTokenRepository {

    private static final String PREFIX = "refresh:";

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void save(String jti, long ttlSeconds) {
        String key = PREFIX + jti;
        redisTemplate.opsForValue().set(
                key,
                "valid",
                Duration.ofSeconds(ttlSeconds)
        );
    }

    @Override
    public boolean exists(String jti) {
        String key = PREFIX + jti;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public void delete(String jti) {
        String key = PREFIX + jti;
        redisTemplate.delete(key);
    }
}
