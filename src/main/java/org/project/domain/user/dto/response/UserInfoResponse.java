package org.project.domain.user.dto.response;

import org.project.domain.user.entity.User;

public record UserInfoResponse(
        Long userId,
        String name,
        String email,
        String profileImageUrl
) {

    public static UserInfoResponse of(User user) {
        return new UserInfoResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getProfileImageUrl()
        );
    }
}
