package org.project.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.project.domain.user.service.JWTService;
import org.project.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "토큰 관련 API")
public class JWTController {

    private final JWTService jwtService;

//    @GetMapping("/access")
//    @Operation(summary = "테스트용 액세스 토큰 발급기",
//            description = "자체 로그인이 없기 때문에 액세스 토큰이 필요한 경우에 해당 메서드를 이용하여 토큰을 발급 받아주세요")
//    public ResponseEntity<ApiResponse<String>> createAccess(HttpServletResponse response){
//        jwtService.createToken(response);
//
//        return ResponseEntity.ok(ApiResponse.ok("테스트용 액세스 토큰이 발급되었습니다. 헤더를 확인해주세요"));
//    }
}
