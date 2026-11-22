package org.project.domain.memo.entity.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.project.domain.memo.entity.SourceMemoSnapshot;

import java.util.List;

// 원본 메모 스냅샷 목록을 JSON 문자열(TEXT)로 저장한다.
// jsonb는 테스트 환경(H2)과 호환되지 않아 TEXT 직렬화를 쓴다 (MemoIdsConverter와 같은 이유).
@Converter
public class SourceMemoSnapshotsConverter implements AttributeConverter<List<SourceMemoSnapshot>, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<SourceMemoSnapshot>> TYPE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(List<SourceMemoSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(snapshots);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("원본 메모 스냅샷 직렬화 실패", e);
        }
    }

    @Override
    public List<SourceMemoSnapshot> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(dbData, TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("원본 메모 스냅샷 역직렬화 실패", e);
        }
    }
}
