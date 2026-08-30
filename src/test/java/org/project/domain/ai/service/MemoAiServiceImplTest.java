package org.project.domain.ai.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.domain.ai.dto.MemoAiOptions;
import org.project.domain.ai.dto.request.MemoAiRequest;
import org.project.domain.ai.dto.response.MemoAiResponse;
import org.project.domain.ai.rag.pipeline.RagPipeline;
import org.project.global.exception.InsufficientRagContextException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemoAiService 테스트")
class MemoAiServiceImplTest {

    @InjectMocks
    private MemoAiServiceImpl memoAiService;

    @Mock
    private RagPipeline ragPipeline;

    @Mock
    private ChatRoomService chatRoomService;

    @Mock
    private ChatMessageService chatMessageService;

    @Mock
    private AiEvaluationService aiEvaluationService;

    @Test
    @DisplayName("AI 응답이 생성되면 사용자 프롬프트와 AI 응답을 저장한다")
    void generate_success_savesTurn() {
        // given
        MemoAiRequest request = MemoAiRequest.of(
                "이 메모들 정리해줘", MemoAiOptions.MERGE, List.of(11L, 12L)
        );
        MemoAiResponse aiResponse = MemoAiResponse.of(
                "주간 회의 정리", "본문", MemoAiOptions.MERGE, List.of(11L, 12L), null
        );
        when(ragPipeline.run(1L, 7L, request)).thenReturn(aiResponse);

        // when
        MemoAiResponse response = memoAiService.generate(1L, 7L, request);

        // then
        assertThat(response.title()).isEqualTo("주간 회의 정리");
        verify(chatMessageService).saveTurn(7L, request, aiResponse);
    }

    @Test
    @DisplayName("컨텍스트가 부족해 안내 응답으로 대체되어도 화면에 노출되므로 저장한다")
    void generate_insufficientContext_savesFallbackTurn() {
        // given
        MemoAiRequest request = MemoAiRequest.of(
                "정리해줘", MemoAiOptions.MERGE, List.of(11L)
        );
        when(ragPipeline.run(1L, 7L, request))
                .thenThrow(new InsufficientRagContextException("메모 내용이 부족합니다"));

        // when
        MemoAiResponse response = memoAiService.generate(1L, 7L, request);

        // then
        assertThat(response.content()).isEqualTo("메모 내용이 부족합니다");
        verify(chatMessageService).saveTurn(eq(7L), eq(request), any(MemoAiResponse.class));
    }

    @Test
    @DisplayName("AI 호출이 실패하면 실패한 턴을 저장하고 예외를 그대로 던진다")
    void generate_pipelineThrows_savesFailedTurnAndRethrows() {
        // given
        MemoAiRequest request = MemoAiRequest.of(
                "정리해줘", MemoAiOptions.MERGE, List.of(11L)
        );
        RuntimeException failure = new RuntimeException("Gemini 호출 실패");
        when(ragPipeline.run(1L, 7L, request)).thenThrow(failure);

        // when & then
        assertThatThrownBy(() -> memoAiService.generate(1L, 7L, request))
                .isSameAs(failure);

        verify(chatMessageService).saveFailedTurn(7L, request);
        verify(chatMessageService, never()).saveTurn(any(), any(), any());
    }

    @Test
    @DisplayName("실패 턴 저장이 실패해도 원래 AI 예외를 그대로 던진다")
    void generate_saveFailedTurnThrows_stillRethrowsOriginal() {
        // given
        MemoAiRequest request = MemoAiRequest.of(
                "정리해줘", MemoAiOptions.MERGE, List.of(11L)
        );
        RuntimeException aiFailure = new RuntimeException("Gemini 호출 실패");
        when(ragPipeline.run(1L, 7L, request)).thenThrow(aiFailure);
        doThrow(new RuntimeException("DB 저장 실패"))
                .when(chatMessageService).saveFailedTurn(7L, request);

        // when & then
        assertThatThrownBy(() -> memoAiService.generate(1L, 7L, request))
                .isSameAs(aiFailure);
    }
}
