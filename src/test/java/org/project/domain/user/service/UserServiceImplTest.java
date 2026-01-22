package org.project.domain.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.domain.user.dto.response.UserInfoResponse;
import org.project.domain.user.entity.User;
import org.project.domain.user.repository.UserRepository;
import org.project.global.exception.domainException.UserException;
import org.project.global.exception.errorcode.UserErrorCode;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserServiceImpl userService;

    @Test
    @DisplayName("유저 정보 조회 성공")
    void getUserInfo_success() {
        // given
        Long userId = 1L;

        User user = User.builder()
                .id(userId)
                .email("test@test.com")
                .name("테스트유저")
                .profileImageUrl("https://image.url")
                .providerName("GOOGLE")
                .build();

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        // when
        UserInfoResponse response = userService.getUserInfo(userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo("test@test.com");
        assertThat(response.name()).isEqualTo("테스트유저");
        assertThat(response.profileImageUrl()).isEqualTo("https://image.url");
    }

    @Test
    @DisplayName("유저가 존재하지 않으면 예외 발생")
    void getUserInfo_userNotFound() {
        // given
        Long userId = 99L;

        given(userRepository.findById(userId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getUserInfo(userId))
                .isInstanceOf(UserException.class)
                .hasMessage(UserErrorCode.NOT_FOUND_USER.getMsg());
    }
}
