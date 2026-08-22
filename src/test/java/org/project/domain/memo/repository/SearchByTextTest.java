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
@DisplayName("searchByText 단어 기반 매칭 + 랭킹(모델 A: 필드 우선 + 온전 키워드 우선)")
class SearchByTextTest {

    @Autowired MemoRepository memoRepository;
    @Autowired UserRepository userRepository;
    @Autowired TestEntityManager em;

    @Test
    @DisplayName("같은 필드 안에서 온전한 키워드(구절) > 부분 매칭 순이고, 필드 우선순위(제목>본문)가 지켜진다")
    void searchByText_ranksPhraseThenPartialThenLowerField() {
        User user = userRepository.save(
                User.createSocialUser("s@test.com", "유저", "p.png", "google"));

        // A: 제목에 "청소 도구" 온전한 구절 → 제목 온전(최상위)
        Memo a = Memo.createMemo("청소 도구 정리함", "청소 도구를 새로 정리했다", user);
        // B: 제목엔 매칭 없고 본문에만 청소·도구가 흩어져 있음 → 본문 부분(최하위)
        Memo b = Memo.createMemo("창고 정리", "빗자루 같은 도구는 청소 창고에 둔다", user);
        // C: 제목에 "청소"만 (구절 아님) → 제목 부분(중간)
        Memo c = Memo.createMemo("청소 완료", "청소가 끝났다", user);
        // D: 매칭 없음 → 제외
        Memo d = Memo.createMemo("회의록", "배포 일정 논의", user);

        em.persist(a); em.persist(b); em.persist(c); em.persist(d);
        em.flush(); em.clear();

        List<Memo> result = memoRepository.searchByText(user.getId(), "청소 도구");

        assertThat(result).extracting(Memo::getTitle)
                .containsExactly("청소 도구 정리함", "청소 완료", "창고 정리");
    }

    @Test
    @DisplayName("필드 우선: 제목 부분 매칭이 본문 온전 키워드보다 항상 위다")
    void searchByText_fieldPriorityBeatsPhrase() {
        User user = userRepository.save(
                User.createSocialUser("fp@test.com", "유저", "p.png", "google"));

        // 가: 제목에 "멋진"만 (부분 매칭) → 제목 부분
        Memo titlePartial = Memo.createMemo("멋진 하루", "오늘의 기록", user);
        // 나: 제목엔 검색어 없고 본문에 "멋진 클러스타" 온전한 구절 → 본문 온전
        Memo bodyPhrase = Memo.createMemo("오늘의 메모", "나는 멋진 클러스타를 만들었다", user);

        em.persist(titlePartial); em.persist(bodyPhrase);
        em.flush(); em.clear();

        List<Memo> result = memoRepository.searchByText(user.getId(), "멋진 클러스타");

        // 제목(부분)이 본문(온전)보다 위
        assertThat(result).extracting(Memo::getTitle)
                .containsExactly("멋진 하루", "오늘의 메모");
    }

    @Test
    @DisplayName("한 단어만 검색해도 그 단어가 든 메모가 매칭된다")
    void searchByText_singleToken_matches() {
        User user = userRepository.save(
                User.createSocialUser("s2@test.com", "유저", "p.png", "google"));
        em.persist(Memo.createMemo("청소 완료", "오늘 청소를 끝냈다", user));
        em.persist(Memo.createMemo("회의", "배포 회의", user));
        em.flush(); em.clear();

        List<Memo> result = memoRepository.searchByText(user.getId(), "청소");

        assertThat(result).extracting(Memo::getTitle).containsExactly("청소 완료");
    }

    @Test
    @DisplayName("매칭되는 메모가 없으면 빈 결과")
    void searchByText_noMatch_returnsEmpty() {
        User user = userRepository.save(
                User.createSocialUser("n@test.com", "유저", "p.png", "google"));
        em.persist(Memo.createMemo("회의록", "배포 일정 논의", user));
        em.flush(); em.clear();

        assertThat(memoRepository.searchByText(user.getId(), "청소 도구")).isEmpty();
    }

    @Test
    @DisplayName("매칭되는 모든 메모를 반환한다(limit 없음)")
    void searchByText_returnsAllMatches() {
        User user = userRepository.save(
                User.createSocialUser("l@test.com", "유저", "p.png", "google"));
        for (int i = 0; i < 5; i++) {
            em.persist(Memo.createMemo("청소 " + i, "청소 내용 " + i, user));
        }
        em.flush(); em.clear();

        assertThat(memoRepository.searchByText(user.getId(), "청소")).hasSize(5);
    }
}
