package org.project.domain.memo.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.MemoFile;
import org.project.domain.user.entity.User;
import org.project.domain.user.repository.UserRepository;
import org.project.global.config.querydsl.QuerydslTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import(QuerydslTestConfig.class)
@DisplayName("MemoFileRepository 테스트")
class MemoFileRepositoryTest {

    @Autowired
    private MemoFileRepository memoFileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("memo 기준으로 MemoFile을 모두 삭제한다")
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

        Memo memo = Memo.createMemo("제목", "내용", user);
        em.persist(memo);

        MemoFile file1 = MemoFile.builder()
                .memo(memo)
                .fileName("file1.png")
                .fileS3Key("s3/file1.png")
                .fileBytes(1_024L)
                .fileExtension("png")
                .filePriority(1)
                .build();

        MemoFile file2 = MemoFile.builder()
                .memo(memo)
                .fileName("file2.png")
                .fileS3Key("s3/file2.png")
                .fileBytes(2_048L)
                .fileExtension("png")
                .filePriority(2)
                .build();

        em.persist(file1);
        em.persist(file2);

        em.flush();
        em.clear();

        // when
        memoFileRepository.deleteByMemo(memo);

        em.flush();
        em.clear();

        // then
        Long count = em.getEntityManager()
                .createQuery(
                        "SELECT COUNT(mf) FROM MemoFile mf WHERE mf.memo.id = :memoId",
                        Long.class
                )
                .setParameter("memoId", memo.getId())
                .getSingleResult();

        assertThat(count).isZero();
    }
}
