package org.project.domain.memo.repository;

import org.project.domain.memo.entity.Memo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MemoRepository extends JpaRepository<Memo,Long> {

    @Query("""
        select distinct m
        from Memo m
        left join fetch m.memoLabels ml
        left join fetch ml.label
        where m.user.id = :userId
        order by m.createdAt desc
    """)
    List<Memo> findAllByUserId(Long userId);

    @Query("""
        select distinct m
        from Memo m
        join m.memoLabels ml
        join ml.label l
        where m.user.id = :userId
          and l.id in :labelIds
        order by m.createdAt desc
    """)
    List<Memo> findAllByUserIdAndLabelIds(Long userId, List<Long> labelIds);
}
