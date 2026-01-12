package org.project.domain.label.entity;

import jakarta.persistence.*;
import lombok.*;
import org.project.domain.memo.entity.MemoLabel;
import org.project.domain.user.entity.User;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "label")
public class Label {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "label_id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "label", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MemoLabel> memoLabels = new ArrayList<>();

    public static Label create(String name, User user) {
        return Label.builder()
                .name(name)
                .user(user)
                .build();
    }

    /**
     * 기본 라벨 목록 생성
     * - 최초 회원가입 시 사용
     */
    public static List<Label> createDefaultLabels(User user) {
        return List.of(
                create("SOPT", user),
                create("학교", user),
                create("책", user),
                create("졸업프로젝트", user)
        );
    }
}
