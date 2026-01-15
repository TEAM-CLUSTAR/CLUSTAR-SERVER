package org.project.domain.memo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.project.domain.memo.dto.request.MemoPresignedUrlRequest;
import org.project.domain.memo.dto.response.MemoPresignedUrlResponse;
import org.project.domain.memo.service.MemoService;
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
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemoController.class)
@AutoConfigureMockMvc(addFilters = false)
class MemoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemoService memoService;

    @MockBean
    private JWTFilter jwtFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("Presigned URL 발급 테스트")
    class IssuePresignedUrlsTest {

        // 커스텀 어노테이션 -> 테스트 전 authntication 객체를 자동 설정하기 위해
        @Retention(RetentionPolicy.RUNTIME)
        @WithSecurityContext(factory = WithMockCustomUserSecurityContextFactory.class)
        @interface WithMockCustomUser {
            long userId() default 1L;
        }

        static class WithMockCustomUserSecurityContextFactory implements WithSecurityContextFactory<WithMockCustomUser> {
            @Override
            public SecurityContext createSecurityContext(WithMockCustomUser annotation) {
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                
                // User 엔티티 생성
                User user = User.builder()
                        .id(annotation.userId())
                        .email("test@example.com")
                        .name("테스트유저")
                        .providerName("google")
                        .profileImageUrl(null)
                        .build();
                
                CustomUserDetails userDetails = new CustomUserDetails(user);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                context.setAuthentication(authentication);
                return context;
            }
        }

        @Test
        @DisplayName("이미지와 파일 모두 포함된 경우 성공")
        @WithMockCustomUser(userId = 2L)
        void issuePresignedUrls_WithImagesAndFiles_Success() throws Exception {
            // given
            Long userId = 2L;

            // 요청 데이터 생성
            MemoPresignedUrlRequest request = new MemoPresignedUrlRequest(
                    List.of(new MemoPresignedUrlRequest.UploadRequest("jpg", 1024L, 1)),
                    List.of(new MemoPresignedUrlRequest.UploadRequest("pdf", 2048L, 1))
            );

            // 예상 응답 데이터 생성
            MemoPresignedUrlResponse expectedResponse = new MemoPresignedUrlResponse(
                    List.of(new MemoPresignedUrlResponse.PresignedUrlResponse("img-key", "http://s3/img", 1024L, "jpg", 1)),
                    List.of(new MemoPresignedUrlResponse.PresignedUrlResponse("file-key", "http://s3/file", 2048L, "pdf", 1))
            );

            when(memoService.issuePresignedUrls(eq(userId), any(MemoPresignedUrlRequest.class)))
                    .thenReturn(expectedResponse);

            // when & then
            mockMvc.perform(post("/api/v1/memo/presigned-urls")
                            .with(securityContext(SecurityContextHolder.getContext()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.images[0].s3Key").value("img-key"))
                    .andExpect(jsonPath("$.data.files[0].s3Key").value("file-key"));

            verify(memoService).issuePresignedUrls(eq(userId), any(MemoPresignedUrlRequest.class));
        }
        

        }
}
