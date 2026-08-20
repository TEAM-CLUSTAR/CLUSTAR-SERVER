package org.project.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.project.domain.user.entity.User;

@Schema(requiredProperties = {"accessToken", "refreshToken", "name", "profileImageUrl"})
public record JwtLoginResponse(
        String accessToken,
        String refreshToken,
//        boolean isRegistered,   // DB 저장된 등록 완료 여부
//        boolean isNewUser,      // 소셜 최초 로그인 여부
        String name,
        @Schema(nullable = true)
        String profileImageUrl
) {

    public static JwtLoginResponse of(User user, String accessToken, String refreshToken) {
        return new JwtLoginResponse(
                accessToken,
                refreshToken,
//                user.isRegistered(),
//                isNewUser,
                user.getName(),
                user.getProfileImageUrl()
        );
    }
}
