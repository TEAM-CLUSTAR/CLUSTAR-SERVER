package org.project.domain.label.entity;

import jakarta.persistence.*;
import lombok.*;
import org.project.domain.memo.entity.MemoLabel;
import org.project.domain.user.entity.User;
import org.project.global.entity.BaseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "label")
public class Label extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "label_id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "color_hex", length = 7)
    private String colorHex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_label_id")
    private Label parent;

    @OneToMany(mappedBy = "parent")
    @Builder.Default
    private List<Label> children = new ArrayList<>();

    @OneToMany(mappedBy = "label", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MemoLabel> memoLabels = new ArrayList<>();

    public static Label create(String name, User user) {
        return Label.builder()
                .name(name)
                .colorHex(generateRandomColorHex())
                .user(user)
                .build();
    }

    public static Label create(String name, User user, Label parent) {
        return Label.builder()
                .name(name)
                .colorHex(generateRandomColorHex())
                .user(user)
                .parent(parent)
                .build();
    }

    public void rename(String name) {
        this.name = name;
    }

    public String getColorHex() {
        if (colorHex == null) {
            colorHex = generateRandomColorHex();
        }
        return colorHex;
    }

    @PrePersist
    @PreUpdate
    private void applyColorHex() {
        if (colorHex == null) {
            colorHex = generateRandomColorHex();
        }
    }

    private static String generateRandomColorHex() {
        return String.format("#%06X", ThreadLocalRandom.current().nextInt(0x1000000));
    }

    /**
     * 기본 라벨 목록 생성
     * - 최초 회원가입 시 사용
     */
    public static List<Label> createDefaultLabels(User user) {
        return List.of(
                create("졸업 프로젝트", user),
                create("교양", user),
                create("SOPT", user),
                create("레퍼런스", user)
        );
    }
}
