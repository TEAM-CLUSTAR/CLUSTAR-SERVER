package org.project.domain.ai.rag.E.retrieve.search;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.domain.memo.config.MemoSearchProperties;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.repository.MemoRepository;
import org.project.domain.user.entity.User;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentMetadata;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemoSearchVectorRetriever 테스트")
class MemoSearchVectorRetrieverTest {

    @InjectMocks
    private MemoSearchVectorRetriever retriever;

    @Mock private VectorStore vectorStore;
    @Mock private MemoRepository memoRepository;
    @Mock private MemoSearchProperties memoSearchProperties;

    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        given(memoSearchProperties.getVectorTopK()).willReturn(10);
        given(memoSearchProperties.getSemanticSimilarityThreshold()).willReturn(0.67);
        given(memoSearchProperties.getSemanticRelativeGap()).willReturn(0.05);
    }

    private Document doc(long memoId, double distance) {
        return new Document("chunk", Map.of(
                "memoId", memoId,
                DocumentMetadata.DISTANCE.value(), distance
        ));
    }

    private Memo memo(long id) {
        Memo memo = Memo.createMemo("메모" + id, "내용", User.builder().id(userId).build());
        ReflectionTestUtils.setField(memo, "id", id);
        return memo;
    }

    @Test
    @DisplayName("휴지통 메모가 1위여도 그 유사도로 상대 기준을 잡지 않아, 살아 있는 메모가 잘리지 않는다")
    void trashedTopCandidate_doesNotCutLiveMemos() {
        // memo 1(휴지통) 유사도 0.90, memo 2(정상) 0.80 — 1위 기준이면 0.85 미만이라 2가 잘렸었다
        given(vectorStore.similaritySearch(any(SearchRequest.class)))
                .willReturn(List.of(doc(1L, 0.10), doc(2L, 0.20)));
        given(memoRepository.findByIdInWithTagsAndNotDeleted(eq(userId), anyList()))
                .willReturn(List.of(memo(2L)));

        List<Memo> result = retriever.retrieve(userId, "검색어", null);

        assertThat(result).extracting(Memo::getId).containsExactly(2L);
    }

    @Test
    @DisplayName("살아 있는 메모끼리는 유사도 순서와 상대 기준을 그대로 적용한다")
    void liveMemos_orderedAndRelativeGapApplied() {
        // 0.90, 0.88은 통과, 0.80은 1위 대비 0.05 넘게 낮아 제외
        given(vectorStore.similaritySearch(any(SearchRequest.class)))
                .willReturn(List.of(doc(3L, 0.10), doc(4L, 0.12), doc(5L, 0.20)));
        given(memoRepository.findByIdInWithTagsAndNotDeleted(eq(userId), anyList()))
                .willReturn(List.of(memo(5L), memo(4L), memo(3L)));

        List<Memo> result = retriever.retrieve(userId, "검색어", null);

        assertThat(result).extracting(Memo::getId).containsExactly(3L, 4L);
    }
}
