package org.project.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.project.domain.user.entity.User;

@Schema(requiredProperties = {"userId", "name", "email", "profileImageUrl"})
public record UserInfoResponse(
        Long userId,
        String name,
        String email,
        @Schema(nullable = true)
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
