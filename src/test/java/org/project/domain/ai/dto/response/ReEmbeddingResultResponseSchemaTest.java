package org.project.domain.ai.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;
import org.project.global.response.ApiResponse;

import static org.assertj.core.api.Assertions.assertThat;

class ReEmbeddingResultResponseSchemaTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void nullable_필드는_null이어도_응답에서_생략되지_않는다() throws Exception {
        ReEmbeddingResultResponse result = new ReEmbeddingResultResponse(1L, true, null, null);

        String json = objectMapper.writeValueAsString(ApiResponse.ok(result));

        assertThat(json)
                .contains("\"imageSucceeded\":null")
                .contains("\"fileSucceeded\":null");
    }

    @Test
    void 재임베딩_결과_스키마는_모든_필드를_required로_명시하고_첨부결과만_nullable로_명시한다() {
        Schema<?> schema = ModelConverters.getInstance()
                .readAllAsResolvedSchema(new io.swagger.v3.core.converter.AnnotatedType(ReEmbeddingResultResponse.class))
                .schema;

        assertThat(schema.getRequired())
                .containsExactlyInAnyOrder("memoId", "textSucceeded", "imageSucceeded", "fileSucceeded");
        assertThat(schema.getProperties().get("imageSucceeded").getNullable()).isTrue();
        assertThat(schema.getProperties().get("fileSucceeded").getNullable()).isTrue();
    }
}
