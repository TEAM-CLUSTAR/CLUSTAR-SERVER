package org.project.domain.user.service;

import org.project.domain.user.dto.response.UserInfoResponse;

public interface UserService {

    UserInfoResponse getUserInfo(Long userId);
}
