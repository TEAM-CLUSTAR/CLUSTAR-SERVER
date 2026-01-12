package org.project.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.project.domain.memo.entity.Memo;
import org.project.global.entity.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Builder
@Table(name = "users")
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "profile_image_url", nullable = true)
    private String profileImageUrl;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "provider_name", nullable = false)
    private String providerName;

    @OneToMany(mappedBy = "user")
    private List<Memo> memos = new ArrayList<>();


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
