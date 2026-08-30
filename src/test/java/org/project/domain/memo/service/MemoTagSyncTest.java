package org.project.domain.memo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.domain.memo.dto.request.MemoCreateRequest;
import org.project.domain.memo.dto.request.MemoUpdateRequest;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.MemoTag;
import org.project.domain.memo.repository.MemoRepository;
import org.project.domain.tag.entity.Tag;
import org.project.domain.tag.repository.TagRepository;
import org.project.domain.user.entity.User;
import org.project.domain.user.repository.UserRepository;
import org.project.global.config.vectorstore.TestVectorStoreConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestVectorStoreConfig.class)
@DisplayName("메모 태그 동기화")
class MemoTagSyncTest {

    @Autowired private MemoService memoService;
    @Autowired private MemoRepository memoRepository;
    @Autowired private TagRepository tagRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    private Long userId;

    @BeforeEach
    void setUp() {
        userId = userRepository.save(User.createSocialUser(
                "tag-sync-" + UUID.randomUUID() + "@test.com",
                "태그유저",
                "profile.png",
                "google"
        )).getId();
    }

    private Long createMemo(List<String> tagNames) {
        return memoService.createMemo(
                userId,
                new MemoCreateRequest("제목", "내용", tagNames, List.of(), List.of())
        ).memoId();
    }

    private void updateTags(Long memoId, List<String> tagNames) {
        memoService.updateMemo(
                userId,
                memoId,
                new MemoUpdateRequest("제목", "내용", tagNames, List.of(), List.of())
        );
    }

    // 저장된 태그를 우선순위 순서대로 읽는다
    private List<String> savedTagNames(Long memoId) {
        return transactionTemplate.execute(status -> {
            Memo memo = memoRepository.findByIdAndNotDeleted(memoId).orElseThrow();
            return memo.getMemoTags().stream()
                    .map(MemoTag::getTag)
                    .map(Tag::getName)
                    .toList();
        });
    }

    @Test
    @DisplayName("태그를 하나만 붙였다 다른 태그로 바꾸면 반영된다")
    void 태그_한개_교체() {
        Long memoId = createMemo(List.of("A"));

        updateTags(memoId, List.of("B"));

        assertThat(savedTagNames(memoId)).containsExactly("B");
    }

    @Test
    @DisplayName("태그가 붙어 있는 메모에 태그를 하나 더 붙일 수 있다")
    void 태그_추가() {
        Long memoId = createMemo(List.of("A"));

        assertThatCode(() -> updateTags(memoId, List.of("A", "B")))
                .doesNotThrowAnyException();

        assertThat(savedTagNames(memoId)).containsExactly("A", "B");
    }

    @Test
    @DisplayName("태그 여러 개 중 하나만 뗄 수 있다")
    void 태그_일부_삭제() {
        Long memoId = createMemo(List.of("A", "B", "C"));

        assertThatCode(() -> updateTags(memoId, List.of("A", "B")))
                .doesNotThrowAnyException();

        assertThat(savedTagNames(memoId)).containsExactly("A", "B");
    }

    @Test
    @DisplayName("유지·추가·삭제가 한 번에 섞여도 반영된다")
    void 태그_혼합_변경() {
        Long memoId = createMemo(List.of("A", "B", "C"));

        assertThatCode(() -> updateTags(memoId, List.of("A", "B", "D")))
                .doesNotThrowAnyException();

        assertThat(savedTagNames(memoId)).containsExactly("A", "B", "D");
    }

    @Test
    @DisplayName("태그 순서를 바꾸면 우선순위가 갱신된다")
    void 태그_순서_변경() {
        Long memoId = createMemo(List.of("A", "B"));

        assertThatCode(() -> updateTags(memoId, List.of("B", "A")))
                .doesNotThrowAnyException();

        assertThat(savedTagNames(memoId)).containsExactly("B", "A");
    }

    @Test
    @DisplayName("태그를 모두 떼면 빈 목록이 된다")
    void 태그_전체_삭제() {
        Long memoId = createMemo(List.of("A", "B"));

        updateTags(memoId, List.of());

        assertThat(savedTagNames(memoId)).isEmpty();
    }

    @Test
    @DisplayName("같은 태그 목록을 반복해서 보내도(자동저장) 실패하지 않는다")
    void 자동저장_반복() {
        Long memoId = createMemo(List.of("A", "B"));

        assertThatCode(() -> {
            updateTags(memoId, List.of("A", "B"));
            updateTags(memoId, List.of("A", "B"));
            updateTags(memoId, List.of("A", "B"));
        }).doesNotThrowAnyException();

        assertThat(savedTagNames(memoId)).containsExactly("A", "B");
    }

    @Test
    @DisplayName("중복된 태그 이름이 섞여 와도 한 번만 저장된다")
    void 중복_이름_요청() {
        Long memoId = createMemo(List.of("A"));

        assertThatCode(() -> updateTags(memoId, List.of("A", "A", "B")))
                .doesNotThrowAnyException();

        assertThat(savedTagNames(memoId)).containsExactly("A", "B");
    }

    @Test
    @DisplayName("태그는 사용자당 이름이 유일해 재사용된다")
    void 태그_재사용() {
        Long memo1 = createMemo(List.of("공용"));
        Long memo2 = createMemo(List.of());

        updateTags(memo2, List.of("공용"));

        assertThat(tagRepository.findAllByNameInAndUser(
                List.of("공용"), userRepository.findById(userId).orElseThrow()))
                .hasSize(1);
        assertThat(savedTagNames(memo1)).containsExactly("공용");
        assertThat(savedTagNames(memo2)).containsExactly("공용");
    }
}
