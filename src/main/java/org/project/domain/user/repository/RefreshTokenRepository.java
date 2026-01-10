package org.project.domain.user.repository;

public interface RefreshTokenRepository {

    void save(String jti, long ttlSeconds);

    boolean exists(String jti);

    void delete(String jti);
}
