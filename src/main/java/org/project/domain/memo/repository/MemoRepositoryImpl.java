package org.project.domain.memo.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.project.domain.label.entity.QLabel;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.QMemo;
import org.project.domain.memo.entity.QMemoLabel;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class MemoRepositoryImpl implements MemoRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Memo> findMemos(
            Long userId,
            List<Long> labelIds,
            LocalDateTime cursorCreatedAt,
            Long cursorMemoId,
            Pageable pageable
    ) {

        QMemo memo = QMemo.memo;
        QMemoLabel memoLabel = QMemoLabel.memoLabel;
        QLabel label = QLabel.label;

        // 전체조회가 아닌 필요한 만큼 조회
        List<Long> memoIds = queryFactory
                .select(memo.id)
                .from(memo)
                .leftJoin(memo.memoLabels, memoLabel)
                .leftJoin(memoLabel.label, label)
                .where(
                        memo.user.id.eq(userId),
                        labelIn(labelIds),
                        cursorCondition(cursorCreatedAt, cursorMemoId)
                )
                .orderBy(
                        memo.createdAt.desc(),
                        memo.id.desc()
                )
                .limit(pageable.getPageSize())
                .fetch();

        // 조회된 id로 실제 메모 페치조인하여 N+1 해결
        return queryFactory
                .selectDistinct(memo)
                .from(memo)
                .leftJoin(memo.memoLabels, memoLabel).fetchJoin()
                .leftJoin(memoLabel.label, label).fetchJoin()
                .where(memo.id.in(memoIds))
                .orderBy(
                        memo.createdAt.desc(),
                        memo.id.desc()
                )
                .fetch();
    }

    /**
     * labelIds가 있을 때
     */
    private BooleanExpression labelIn(List<Long> labelIds) {
        if (labelIds == null || labelIds.isEmpty()) {
            return null;
        }
        return QMemoLabel.memoLabel.label.id.in(labelIds);
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
