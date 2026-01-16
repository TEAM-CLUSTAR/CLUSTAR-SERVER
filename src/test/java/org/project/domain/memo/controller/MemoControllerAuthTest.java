package org.project.domain.memo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.domain.memo.dto.request.MemoCreateRequest;
import org.project.domain.memo.dto.request.MemoPresignedUrlRequest;
import org.project.domain.memo.service.MemoService;
import org.project.global.security.filter.JWTFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemoController.class)
@AutoConfigureMockMvc(addFilters = true) 
@DisplayName("메모 컨트롤러 인증 테스트")
class MemoControllerAuthTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemoService memoService;

    @MockBean
    private JWTFilter jwtFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("인증되지 않은 사용자는 presignedUrl 요청에 실패해야 한다.")
    void issuePresignedUrls_Unauthorized_Fail() throws Exception {
        // given
        MemoPresignedUrlRequest request = new MemoPresignedUrlRequest(
                List.of(new MemoPresignedUrlRequest.UploadRequest("jpg", 1024L, 1)),
                List.of()
        );

        // when & then
        mockMvc.perform(post("/api/v1/memo/presigned-urls")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verify(memoService, never()).issuePresignedUrls(anyLong(), any());
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 메모 작성에 실패해야 한다.")
    void createMemo_Unauthorized_Fail() throws Exception {
        // given
        MemoCreateRequest request = new MemoCreateRequest(
                "제목",
                "내용",
                List.of(),
                List.of(),
                List.of()
        );

        // when & then
        mockMvc.perform(post("/api/v1/memo")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verify(memoService, never()).createMemo(anyLong(), any());
    }
}
