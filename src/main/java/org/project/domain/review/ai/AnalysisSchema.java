package org.project.domain.review.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.project.domain.review.entity.FindingCategory;

import java.util.Arrays;

// 분석 응답 JSON 스키마를 Gemini에 넘길 모양으로 고친다.
// FindingDraft.category는 모르는 값이 와도 그 카드만 버리려고 문자열로 받지만,
// AI에게는 세 값만 쓰도록 스키마에 enum으로 묶어 둔다
final class AnalysisSchema {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AnalysisSchema() {
    }

    static String withCategoryEnum(String jsonSchema) {
        try {
            ObjectNode root = (ObjectNode) MAPPER.readTree(jsonSchema);
            // Gemini 스키마는 메타 정보 키를 받지 않는다
            root.remove("$schema");

            ObjectNode category = (ObjectNode) root.at("/properties/findings/items/properties/category");
            ArrayNode values = category.putArray("enum");
            Arrays.stream(FindingCategory.values()).map(Enum::name).forEach(values::add);

            return MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException | ClassCastException e) {
            throw new IllegalStateException("분석 응답 스키마를 만들 수 없음", e);
        }
    }
}
