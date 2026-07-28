package org.project.domain.ai.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.domain.ai.entity.EmbeddingFailure;
import org.project.domain.ai.repository.EmbeddingFailureRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingFailureHandler {

    private final EmbeddingFailureRepository embeddingFailureRepository;

    // 호출될 때마다 무조건 새 트랜잭션을 열어서 실패 기록을 DB에 커밋함
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long memoId, String embeddingType, Exception e) {
        log.error("임베딩 실패 - DB 기록 저장: memoId={}, type={}, error={}", memoId, embeddingType, e.getMessage());
        embeddingFailureRepository.save(EmbeddingFailure.create(memoId, embeddingType, e));
    }

    // 같은 메모라도 text/image/file 중 실제로 성공한 타입의 미해결 기록만 해결 처리한다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resolveType(Long memoId, String embeddingType) {
        List<EmbeddingFailure> unresolved =
                embeddingFailureRepository.findByMemoIdAndEmbeddingTypeAndIsResolvedFalse(memoId, embeddingType);
        unresolved.forEach(EmbeddingFailure::resolve);
    }
}
