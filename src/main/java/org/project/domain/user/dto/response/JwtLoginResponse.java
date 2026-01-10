package org.project.domain.user.dto.response;

import org.project.domain.user.entity.User;

public record JwtLoginResponse(
        String accessToken,
        String refreshToken,
//        boolean isRegistered,   // DB 저장된 등록 완료 여부
//        boolean isNewUser,      // 소셜 최초 로그인 여부
        String name,
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
