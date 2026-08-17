package org.project.domain.memo.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.domain.memo.entity.Memo;
import org.project.domain.user.entity.User;
import org.project.domain.user.repository.UserRepository;
import org.project.global.config.querydsl.QuerydslTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(QuerydslTestConfig.class)
@DisplayName("searchByText 단어 기반 매칭 + 랭킹")
class SearchByTextTest {

    @Autowired MemoRepository memoRepository;
    @Autowired UserRepository userRepository;
    @Autowired TestEntityManager em;

    @Test
    @DisplayName("다단어 검색어는 흩어져 있어도 매칭되고, 정확한 구절 > 두 단어 > 한 단어 순으로 정렬된다")
    void searchByText_ranksPhraseThenBothTokensThenSingle() {
        User user = userRepository.save(
                User.createSocialUser("s@test.com", "유저", "p.png", "google"));

        // A: 정확한 구절 "청소 도구" 포함 → 최상위
        Memo a = Memo.createMemo("청소 도구 정리함", "청소 도구를 새로 정리했다", user);
        // B: 청소·도구 둘 다 있지만 흩어짐 → 중간
        Memo b = Memo.createMemo("창고 정리", "빗자루 같은 도구는 청소 창고에 둔다", user);
        // C: 한 단어(청소)만 → 하위
        Memo c = Memo.createMemo("청소 완료", "청소가 끝났다", user);
        // D: 매칭 없음 → 제외
        Memo d = Memo.createMemo("회의록", "배포 일정 논의", user);

        em.persist(a); em.persist(b); em.persist(c); em.persist(d);
        em.flush(); em.clear();

        List<Memo> result = memoRepository.searchByText(user.getId(), "청소 도구", 3);

        assertThat(result).extracting(Memo::getTitle)
                .containsExactly("청소 도구 정리함", "창고 정리", "청소 완료");
    }

    @Test
    @DisplayName("한 단어만 검색해도 그 단어가 든 메모가 매칭된다")
    void searchByText_singleToken_matches() {
        User user = userRepository.save(
                User.createSocialUser("s2@test.com", "유저", "p.png", "google"));
        em.persist(Memo.createMemo("청소 완료", "오늘 청소를 끝냈다", user));
        em.persist(Memo.createMemo("회의", "배포 회의", user));
        em.flush(); em.clear();

        List<Memo> result = memoRepository.searchByText(user.getId(), "청소", 3);

        assertThat(result).extracting(Memo::getTitle).containsExactly("청소 완료");
    }

    @Test
    @DisplayName("매칭되는 메모가 없으면 빈 결과")
    void searchByText_noMatch_returnsEmpty() {
        User user = userRepository.save(
                User.createSocialUser("n@test.com", "유저", "p.png", "google"));
        em.persist(Memo.createMemo("회의록", "배포 일정 논의", user));
        em.flush(); em.clear();

        assertThat(memoRepository.searchByText(user.getId(), "청소 도구", 3)).isEmpty();
    }

    @Test
    @DisplayName("매칭이 많아도 상위 limit 개수만 반환한다")
    void searchByText_respectsLimit() {
        User user = userRepository.save(
                User.createSocialUser("l@test.com", "유저", "p.png", "google"));
        for (int i = 0; i < 5; i++) {
            em.persist(Memo.createMemo("청소 " + i, "청소 내용 " + i, user));
        }
        em.flush(); em.clear();

        assertThat(memoRepository.searchByText(user.getId(), "청소", 3)).hasSize(3);
    }
}
