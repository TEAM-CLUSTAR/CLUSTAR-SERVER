package org.project.domain.user.service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.project.global.util.JWTUtil;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JWTService {

    private final JWTUtil jwtUtil;

    // 토큰 발급기를 위한 메서드입니다
//    public void createToken(HttpServletResponse response) {
//
//        String access = jwtUtil.generateAccessToken(1L);
//        response.setHeader("access-token", access);
//    }
}
