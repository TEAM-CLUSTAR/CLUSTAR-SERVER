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
    void deleteByMemo_success() {
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
        memoImageRepository.deleteByMemo(memo);
        em.flush();
        em.clear();

        // then
        List<MemoImage> result =
                memoImageRepository.findAll();

        assertThat(result).isEmpty();
    }
}
