package org.project.domain.memo.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.MemoImage;
import org.project.domain.user.entity.User;
import org.project.domain.user.repository.UserRepository;
import org.project.global.config.querydsl.QuerydslTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@ActiveProfiles("test")
@Import(QuerydslTestConfig.class)
@DisplayName("MemoImageRepository 테스트")
class MemoImageRepositoryTest {

    @Autowired
    private MemoRepository memoRepository;

    @Autowired
    private MemoImageRepository memoImageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager em;


    @Test
    @DisplayName("memo로 연결된 MemoImage가 모두 삭제된다")
    void deleteByMemoIdIn_success() {
        // given
        User user = userRepository.save(
                User.createSocialUser(
                        "test" + UUID.randomUUID() + "@test.com",
                        "테스트 유저",
                        "profile.png",
                        "google"
                )
        );

        Memo memo = memoRepository.save(
                Memo.createMemo("제목", "내용", user)
        );

        MemoImage image1 = MemoImage.builder()
                .memo(memo)
                .imageName("img1.png")
                .imageS3Key("s3/img1.png")
                .imagePriority(1)
                .build();

        MemoImage image2 = MemoImage.builder()
                .memo(memo)
                .imageName("img2.png")
                .imageS3Key("s3/img2.png")
                .imagePriority(2)
                .build();

        memoImageRepository.save(image1);
        memoImageRepository.save(image2);

        em.flush();
        em.clear();

        // when
        memoImageRepository.deleteByMemoIdIn(List.of(memo.getId()));
        em.flush();
        em.clear();

        // then
        List<MemoImage> result =
                memoImageRepository.findAll();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("deleteByMemoIdIn은 대상 메모들의 이미지만 지우고 다른 메모 이미지는 남긴다")
    void deleteByMemoIdIn_keepsNonTarget() {
        User user = userRepository.save(User.createSocialUser(
                "batch" + UUID.randomUUID() + "@test.com", "테스트 유저", "profile.png", "google"));
        Memo target1 = Memo.createMemo("대상1", "내용", user);
        Memo target2 = Memo.createMemo("대상2", "내용", user);
        Memo other = Memo.createMemo("비대상", "내용", user);
        em.persist(target1);
        em.persist(target2);
        em.persist(other);
        for (Memo m : List.of(target1, target2, other)) {
            em.persist(MemoImage.builder().memo(m).imageName("i.png")
                    .imageS3Key("s3/" + m.getTitle() + ".png").imagePriority(1).build());
        }
        em.flush();
        em.clear();

        memoImageRepository.deleteByMemoIdIn(List.of(target1.getId(), target2.getId()));
        em.flush();
        em.clear();

        assertThat(memoImageRepository.findAll())
                .extracting(image -> image.getMemo().getId())
                .containsExactly(other.getId());
    }
}
