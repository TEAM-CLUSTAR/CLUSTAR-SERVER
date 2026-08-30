package org.project.global.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.global.exception.domainException.AiException;
import org.project.global.exception.errorcode.AiErrorCode;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MemoContentParser 테스트")
class MemoContentParserTest {

    @Test
    @DisplayName("첫 줄을 제목으로, 나머지를 본문으로 분리한다")
    void parseTitleAndContent_success() {
        // when
        MemoContentParser.ParsedMemoContent parsed =
                MemoContentParser.parseTitleAndContent("주간 회의 정리\n\n본문 첫 줄\n본문 둘째 줄");

        // then
        assertThat(parsed.title()).isEqualTo("주간 회의 정리");
        assertThat(parsed.content()).isEqualTo("본문 첫 줄\n본문 둘째 줄");
    }

    @Test
    @DisplayName("AI가 형식을 어겨 한 줄로 응답하면 재시도 가능한 503으로 처리한다")
    void parseTitleAndContent_singleLine_isRetryableServerError() {
        assertThatThrownBy(() ->
                MemoContentParser.parseTitleAndContent("정리할 내용이 없습니다"))
                .isInstanceOf(AiException.class)
                .extracting(e -> ((AiException) e).getErrorCode().getStatus())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("AI 응답이 null이면 재시도 가능한 503으로 처리한다")
    void parseTitleAndContent_null_isRetryableServerError() {
        assertThatThrownBy(() -> MemoContentParser.parseTitleAndContent(null))
                .isInstanceOf(AiException.class)
                .extracting(e -> ((AiException) e).getErrorCode().getStatus())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("AI 응답이 비어 있으면 재시도 가능한 503으로 처리한다")
    void parseTitleAndContent_blank_isRetryableServerError() {
        assertThatThrownBy(() -> MemoContentParser.parseTitleAndContent("   \n  "))
                .isInstanceOf(AiException.class)
                .extracting(e -> ((AiException) e).getErrorCode().getStatus())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("AI 응답 형식 오류는 사용자가 읽을 수 있는 안내 문구를 사용한다")
    void aiResponseFormatErrors_haveUserFacingMessage() {
        for (AiErrorCode code : new AiErrorCode[]{
                AiErrorCode.AI_RESPONSE_NULL,
                AiErrorCode.AI_RESPONSE_EMPTY,
                AiErrorCode.AI_TITLE_EXTRACTION_FAILED,
                AiErrorCode.AI_TITLE_EMPTY
        }) {
            assertThat(code.getMsg())
                    .as("%s 의 메시지", code)
                    .doesNotContain("null")
                    .contains("다시 시도");
        }
    }
}
