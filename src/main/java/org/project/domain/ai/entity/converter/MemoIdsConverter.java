package org.project.domain.ai.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.List;

/**
 * memoIds(List&lt;Long&gt;)를 콤마로 구분된 문자열로 저장한다.
 * <p>
 * 조인 테이블(@ElementCollection)은 메시지 목록 조회 시 N+1을 유발하고,
 * jsonb는 테스트 환경(H2)과 호환되지 않아 TEXT 직렬화를 사용한다.
 */
@Converter
public class MemoIdsConverter implements AttributeConverter<List<Long>, String> {

    private static final String DELIMITER = ",";

    // 저장 시: 엔티티 필드 -> DB 컬럼
    @Override
    public String convertToDatabaseColumn(List<Long> memoIds) {
        if (memoIds == null || memoIds.isEmpty()) {
            return null;
        }

        return memoIds.stream()
                .map(String::valueOf)
                .reduce((a, b) -> a + DELIMITER + b)
                .orElse(null);
    }

    // 조회 시: DB 컬럼 -> 엔티티 필드
    @Override
    public List<Long> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }

        return Arrays.stream(dbData.split(DELIMITER))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();
    }
}
