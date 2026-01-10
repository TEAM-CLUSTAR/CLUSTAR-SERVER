package org.project.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.project.global.entity.BaseEntity;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Builder
@Table(name = "users")
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "profile_image_url", nullable = false)
    private String profileImageUrl;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "provider_name", nullable = false)
    private String providerName;


    /**
     * 소셜 로그인 사용자 생성 (최초 회원가입 시)
     * - email / name / profileImageUrl은 소셜에서 내려오는 값
     * - providerName은 OAuth 제공회사 이름
     */
    public static User createSocialUser(String email, String name, String profileImageUrl, String providerName) {
        return User.builder()
                .email(email)
                .name(name)
                .profileImageUrl(profileImageUrl)
                .providerName(providerName)
                .build();
    }
}
