package org.project.global.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.project.global.exception.domainException.LoginException;
import org.project.global.exception.errorcode.LoginErrorCode;
import org.project.global.security.properties.JwtProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JWTUtil {

    private final JwtProperties jwtProperties;
    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getExpireLength());

        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(userId.toString())
//                .claim("isRegistered", isRegistered)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getRefreshExpireLength());

        return Jwts.builder()
                .setId(UUID.randomUUID().toString()) // JTI 추가
                .setSubject(userId.toString())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return Long.parseLong(claims.getSubject());
    }

    // 토큰의 식별자를 파싱하는 메서드
    public String getJti(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getId();
    }

    public long getRemainingExpiration(String token) {
        Date expiration = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();

        return (expiration.getTime() - System.currentTimeMillis()) / 1000;
    }

    // RefreshToken 검증
    public Long validateRefreshToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // 만료 체크 (만료된 경우 ExpiredJwtException 발생)
            Date expiration = claims.getExpiration();
            if (expiration.before(new Date())) {
                throw new LoginException(LoginErrorCode.REFRESH_TOKEN_EXPIRED);
            }

            // userId(subject) 반환
            return Long.parseLong(claims.getSubject());

        } catch (JwtException | IllegalArgumentException e) {
            throw new LoginException(LoginErrorCode.INVALID_REFRESH_TOKEN);
        }
    }
}

