package org.project.domain.ai.rag;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.domain.ai.dto.MemoAiOptions;
import org.project.domain.ai.dto.request.MemoAiRequest;
import org.project.domain.ai.dto.response.MemoAiResponse;
import org.project.domain.ai.rag.D.query.RagQueryHandler;
import org.project.domain.ai.rag.D.query.dto.RagQuery;
import org.project.domain.ai.rag.E.retrieve.RagRetriever;
import org.project.domain.ai.rag.F.augment.RagAugmenter;
import org.project.domain.ai.rag.F.augment.dto.RagPrompt;
import org.project.domain.ai.rag.G.generate.RagGenerator;
import org.project.domain.ai.rag.pipeline.DefaultRagPipeline;
import org.project.global.config.chatModel.TestChatModelConfig;
import org.project.global.config.chatModel.YesChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RagPipelineRelevancyTest {

    @InjectMocks
    DefaultRagPipeline ragPipeline;

    ChatClient.Builder chatClientBuilder =
            ChatClient.builder(new YesChatModel());

    @Mock
    RagQueryHandler ragQueryHandler;

    @Mock
    RagRetriever ragRetriever;

    @Mock
    RagAugmenter ragAugmenter;

    @Mock
    RagGenerator ragGenerator;

    @Mock
    VectorStore vectorStore;

    @Test
    void rag_response_should_be_relevant_to_context() {

        // given
        MemoAiRequest request = MemoAiRequest.of(
                "이 메모 뭐야?",
                MemoAiOptions.MERGE,
                List.of(1L)
        );

        given(ragQueryHandler.handle(any(Long.class), any(MemoAiRequest.class)))
                .willReturn(
                        RagQuery.of(
                                1L,
                                request.option(),
                                request.userPrompt(),
                                request.memoIds()
                        )
                );

        given(ragAugmenter.augment(any(RagQuery.class), anyList()))
                .willReturn(
                        RagPrompt.of(
                                "SYSTEM",
                                "이 문서는 메모의 핵심을 요약한 내용입니다.",
                                request.userPrompt()
                        )
                );

        given(ragGenerator.generate(any()))
                .willReturn("이 문서는 메모의 핵심을 요약한 내용입니다.");

        // when
        MemoAiResponse response = ragPipeline.run(1L, 1L, request);

        // then
        EvaluationRequest evaluationRequest =
                new EvaluationRequest(
                        request.userPrompt(),
                        List.of(new Document(response.usedPrompt())),
                        response.content()
                );

        RelevancyEvaluator evaluator =
                new RelevancyEvaluator(chatClientBuilder);

        EvaluationResponse evaluationResponse =
                evaluator.evaluate(evaluationRequest);

        assertThat(evaluationResponse.isPass()).isTrue();
    }
}
