package org.project.domain.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.domain.ai.dto.MemoAiOptions;
import org.project.domain.ai.dto.request.MemoAiRequest;
import org.project.domain.ai.service.MemoAiService;
import org.project.domain.user.dto.CustomUserDetails;
import org.project.domain.user.entity.User;
import org.project.global.security.filter.JWTFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemoAiController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("MemoAiController 테스트")
class MemoAiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MemoAiService memoAiService;

    @MockBean
    private org.project.domain.ai.rag.pipeline.RagPipeline ragPipeline;

    @MockBean
    private org.project.domain.ai.service.AiEvaluationService aiEvaluationService;

    @MockBean
    private org.project.domain.ai.service.ChatRoomService chatRoomService;

    @MockBean
    private org.project.domain.ai.service.DummyService dummyService;

    @MockBean
    private JWTFilter jwtFilter;

    @Test
    @DisplayName("userPrompt가 비어 있으면 400을 반환하고 AI 호출로 넘어가지 않는다")
    @WithMockCustomUser
    void generateMemoAi_blankUserPrompt_returnsBadRequest() throws Exception {
        // given
        MemoAiRequest request = MemoAiRequest.of("   ", MemoAiOptions.MERGE, List.of(11L));

        // when & then
        mockMvc.perform(post("/api/v1/chat-rooms/7/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(memoAiService, never()).generate(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("userPrompt가 null이면 400을 반환하고 AI 호출로 넘어가지 않는다")
    @WithMockCustomUser
    void generateMemoAi_nullUserPrompt_returnsBadRequest() throws Exception {
        // given
        MemoAiRequest request = MemoAiRequest.of(null, MemoAiOptions.MERGE, List.of(11L));

        // when & then
        mockMvc.perform(post("/api/v1/chat-rooms/7/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(memoAiService, never()).generate(anyLong(), anyLong(), any());
    }

    @Retention(RetentionPolicy.RUNTIME)
    @WithSecurityContext(factory = WithMockCustomUserSecurityContextFactory.class)
    @interface WithMockCustomUser {
        long userId() default 1L;
    }

    static class WithMockCustomUserSecurityContextFactory
            implements WithSecurityContextFactory<WithMockCustomUser> {
        @Override
        public SecurityContext createSecurityContext(WithMockCustomUser annotation) {
            SecurityContext context = SecurityContextHolder.createEmptyContext();

            User user = User.builder()
                    .id(annotation.userId())
                    .email("test@example.com")
                    .name("테스트유저")
                    .providerName("google")
                    .profileImageUrl(null)
                    .build();

            CustomUserDetails userDetails = new CustomUserDetails(user);
            context.setAuthentication(new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
            ));
            return context;
        }
    }
}
