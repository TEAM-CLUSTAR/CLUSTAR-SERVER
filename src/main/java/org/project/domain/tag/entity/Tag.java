package org.project.domain.tag.entity;

import jakarta.persistence.*;
import lombok.*;
import org.project.domain.tag.util.TagColorPalette;
import org.project.domain.memo.entity.MemoTag;
import org.project.domain.user.entity.User;
import org.project.global.entity.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "tag",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "name"})
)
public class Tag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "color_hex", nullable = false, length = 7)
    private String colorHex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_tag_id")
    private Tag parent;

    @OneToMany(mappedBy = "parent")
    @Builder.Default
    private List<Tag> children = new ArrayList<>();

    @OneToMany(mappedBy = "tag", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MemoTag> memoTags = new ArrayList<>();

    public static Tag create(String name, User user) {
        return Tag.builder()
                .name(name)
                .colorHex(TagColorPalette.randomColor())
                .user(user)
                .build();
    }

    public static Tag create(String name, User user, Tag parent) {
        return Tag.builder()
                .name(name)
                .colorHex(TagColorPalette.randomColor())
                .user(user)
                .parent(parent)
                .build();
    }

    public void rename(String name) {
        this.name = name;
    }

    public String getColorHex() {
        return colorHex;
    }

    @PrePersist
    @PreUpdate
    private void applyColorHex() {
        if (colorHex == null) {
            colorHex = TagColorPalette.randomColor();
        }
    }

    /**
     * 기본 태그 목록 생성
     * - 최초 회원가입 시 사용
     */
    public static List<Tag> createDefaultTags(User user) {
        return List.of(
                create("졸업 프로젝트", user),
                create("교양", user),
                create("SOPT", user),
                create("레퍼런스", user)
        );
    }
}
