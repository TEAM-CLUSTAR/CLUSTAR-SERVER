package org.project.domain.memo.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.project.global.util.SearchQueryTokenizer;
import org.project.domain.tag.entity.QTag;
import org.project.domain.tag.entity.Tag;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.QMemo;
import org.project.domain.memo.entity.QMemoTag;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
public class MemoRepositoryImpl implements MemoRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Memo> findMemos(
            Long userId,
            List<Long> tagIds,
            LocalDateTime cursorCreatedAt,
            Long cursorMemoId,
            Pageable pageable
    ) {

        QMemo memo = QMemo.memo;
        QMemoTag memoTag = QMemoTag.memoTag;
        QTag tag = QTag.tag;

        // 전체조회가 아닌 필요한 만큼 조회
        List<Long> memoIds = queryFactory
                .select(memo.id)
                .from(memo)
                .leftJoin(memo.memoTags, memoTag)
                .leftJoin(memoTag.tag, tag)
                .where(
                        memo.user.id.eq(userId),
                        memo.isDeleted.eq(false),
                        tagIn(tagIds),
                        cursorCondition(cursorCreatedAt, cursorMemoId)
                )
                .groupBy(memo.id)
                .orderBy(
                        memo.createdAt.desc(),
                        memo.id.desc()
                )
                .limit(pageable.getPageSize())
                .fetch();

        if (memoIds.isEmpty()) {
            return List.of();
        }

        // 조회된 id로 실제 메모 페치조인하여 N+1 해결
        return queryFactory
                .selectDistinct(memo)
                .from(memo)
                .leftJoin(memo.memoTags, memoTag).fetchJoin()
                .leftJoin(memoTag.tag, tag).fetchJoin()
                .where(memo.id.in(memoIds))
                .orderBy(
                        memo.createdAt.desc(),
                        memo.id.desc()
                )
                .fetch();
    }


    @Override
    public List<Memo> findAiMemos(
            Long userId,
            List<Long> tagIds,
            LocalDateTime cursorCreatedAt,
            Long cursorMemoId,
            Pageable pageable
    ) {

        QMemo memo = QMemo.memo;
        QMemoTag memoTag = QMemoTag.memoTag;
        QTag tag = QTag.tag;

        // 전체조회가 아닌 필요한 만큼 조회
        List<Long> memoIds = queryFactory
                .select(memo.id)
                .from(memo)
                .leftJoin(memo.memoTags, memoTag)
                .leftJoin(memoTag.tag, tag)
                .where(
                        memo.user.id.eq(userId),
                        memo.isDeleted.eq(false),
                        memo.isAiGenerated.eq(true),
                        tagIn(tagIds),
                        cursorCondition(cursorCreatedAt, cursorMemoId)
                )
                .groupBy(memo.id)
                .orderBy(
                        memo.createdAt.desc(),
                        memo.id.desc()
                )
                .limit(pageable.getPageSize())
                .fetch();

        if (memoIds.isEmpty()) {
            return List.of();
        }

        // 조회된 id로 실제 메모 페치조인하여 N+1 해결
        return queryFactory
                .selectDistinct(memo)
                .from(memo)
                .leftJoin(memo.memoTags, memoTag).fetchJoin()
                .leftJoin(memoTag.tag, tag).fetchJoin()
                .where(memo.id.in(memoIds))
                .orderBy(
                        memo.createdAt.desc(),
                        memo.id.desc()
                )
                .fetch();
    }

    @Override
    public List<Memo> searchByText(Long userId, String query) {
        QMemo memo = QMemo.memo;
        QMemoTag memoTag = QMemoTag.memoTag;
        QTag tag = QTag.tag;

        // 검색어를 단어로 분리(공백 기준). 단어가 없으면 결과 없음.
        List<String> tokens = SearchQueryTokenizer.tokenize(query);
        if (tokens.isEmpty()) {
            return List.of();
        }

        // 매칭: 각 토큰이 제목/내용/태그 중 하나라도 포함되면 후보(토큰 간 OR — 한 단어만 맞아도 포함)
        BooleanBuilder tokenMatch = new BooleanBuilder();
        for (String token : tokens) {
            tokenMatch.or(memo.title.containsIgnoreCase(token))
                    .or(memo.content.containsIgnoreCase(token))
                    .or(tag.name.containsIgnoreCase(token));
        }

        List<Memo> candidates = queryFactory
                .selectDistinct(memo)
                .from(memo)
                .leftJoin(memo.memoTags, memoTag).fetchJoin()
                .leftJoin(memoTag.tag, tag).fetchJoin()
                .where(
                        memo.user.id.eq(userId),
                        memo.isDeleted.eq(false),
                        tokenMatch
                )
                .fetch();

        // 6: 제목 온전 > 5: 제목 부분 > 4: 태그 온전 > 3: 태그 부분 > 2: 본문 온전 > 1: 본문 부분
        // 매칭되는 모든 메모를 반환(무한 스크롤 가능하게)
        String phrase = String.join(" ", tokens).toLowerCase();
        List<Memo> ranked = new ArrayList<>(candidates);
        ranked.sort(
                Comparator.comparingInt((Memo m) -> matchRank(m, tokens, phrase)).reversed()
                        .thenComparing(Memo::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Memo::getId, Comparator.reverseOrder())
        );

        return ranked;
    }

    // 필드 우선순위(제목>태그>본문) + 필드 내 "온전 키워드 우선"을 하나의 순위값으로 환산. 높을수록 상위.
    private int matchRank(Memo m, List<String> tokens, String phrase) {
        String title = m.getTitle() == null ? "" : m.getTitle().toLowerCase();
        String content = m.getContent() == null ? "" : m.getContent().toLowerCase();
        List<String> tagNames = m.getTags().stream()
                .map(Tag::getName)
                .filter(n -> n != null)
                .map(String::toLowerCase)
                .toList();

        if (title.contains(phrase)) return 6;                                        // 제목 - 온전한 키워드
        if (containsAnyToken(title, tokens)) return 5;                               // 제목 - 부분 매칭
        if (tagNames.stream().anyMatch(n -> n.contains(phrase))) return 4;           // 태그 - 온전한 키워드
        if (tagNames.stream().anyMatch(n -> containsAnyToken(n, tokens))) return 3;  // 태그 - 부분 매칭
        if (content.contains(phrase)) return 2;                                      // 본문 - 온전한 키워드
        if (containsAnyToken(content, tokens)) return 1;                             // 본문 - 부분 매칭
        return 0; // 후보 쿼리를 통과했으므로 정상적으로는 도달하지 않음
    }

    private boolean containsAnyToken(String field, List<String> tokens) {
        for (String token : tokens) {
            if (field.contains(token.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Memo> findRecentViewed(Long userId, int limit) {
        QMemo memo = QMemo.memo;
        QMemoTag memoTag = QMemoTag.memoTag;
        QTag tag = QTag.tag;

        // 열람 이력이 있는(lastViewedAt != null) 메모를 최신 열람순으로 상위 limit개.
        // 컬렉션 fetchJoin + limit의 인메모리 페이징을 피하려고 id 먼저 조회 후 페치조인(findMemos와 동일 패턴).
        List<Long> memoIds = queryFactory
                .select(memo.id)
                .from(memo)
                .where(
                        memo.user.id.eq(userId),
                        memo.isDeleted.eq(false),
                        memo.lastViewedAt.isNotNull()
                )
                .orderBy(memo.lastViewedAt.desc(), memo.id.desc())
                .limit(limit)
                .fetch();

        if (memoIds.isEmpty()) {
            return List.of();
        }

        return queryFactory
                .selectDistinct(memo)
                .from(memo)
                .leftJoin(memo.memoTags, memoTag).fetchJoin()
                .leftJoin(memoTag.tag, tag).fetchJoin()
                .where(memo.id.in(memoIds))
                .orderBy(memo.lastViewedAt.desc(), memo.id.desc())
                .fetch();
    }

    /**
     * tagIds가 있을 때
     */
    private BooleanExpression tagIn(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return null;
        }
        return QMemoTag.memoTag.tag.id.in(tagIds);
    }

    /**
     * 커서 조건
     */
    private BooleanExpression cursorCondition(
            LocalDateTime cursorCreatedAt,
            Long cursorMemoId
    ) {
        if (cursorCreatedAt == null || cursorMemoId == null) {
            return null;
        }

        QMemo memo = QMemo.memo;

        return memo.createdAt.lt(cursorCreatedAt)
                .or(
                        memo.createdAt.eq(cursorCreatedAt)
                                .and(memo.id.lt(cursorMemoId))
                );
    }
}
