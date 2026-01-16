package org.project.domain.memo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.project.domain.memo.dto.request.MemoCreateRequest;
import org.project.domain.memo.dto.request.MemoPresignedUrlRequest;
import org.project.domain.memo.dto.response.MemoPresignedUrlResponse;
import org.project.domain.memo.dto.response.MemoResponse;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
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


    @Nested
    @DisplayName("Presigned URL 발급 테스트")
    class IssuePresignedUrlsTest {


        @Test
        @DisplayName("이미지와 파일 모두 포함된 경우 성공해야 한다.")
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
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print()) // 테스트 실행 시 콘솔 창에 요청 & 응답 상세 출력
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.images[0].s3Key").value("img-key"))
                    .andExpect(jsonPath("$.data.files[0].s3Key").value("file-key"));

            verify(memoService).issuePresignedUrls(eq(userId), any(MemoPresignedUrlRequest.class));
        }

        @Test
        @DisplayName("이미지만 있는 경우도 성공해야 한다.")
        @WithMockCustomUser(userId = 2L)
        void issuePresignedUrls_OnlyImages_Success() throws Exception {
            // given
            Long userId = 2L;

            MemoPresignedUrlRequest request = new MemoPresignedUrlRequest(
                    List.of(
                            new MemoPresignedUrlRequest.UploadRequest("png", 2048L, 1)
                    ),
                    List.of()  // 파일 없음
            );

            MemoPresignedUrlResponse expectedResponse = new MemoPresignedUrlResponse(
                    List.of(
                            new MemoPresignedUrlResponse.PresignedUrlResponse(
                                    "img-key", "http://s3/img", 2048L, "png", 1)
                    ),
                    List.of()
            );

            when(memoService.issuePresignedUrls(eq(userId), any(MemoPresignedUrlRequest.class)))
                    .thenReturn(expectedResponse);

            // when & then
            mockMvc.perform(post("/api/v1/memo/presigned-urls")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.images").isArray())
                    .andExpect(jsonPath("$.data.images.length()").value(1))
                    .andExpect(jsonPath("$.data.images[0].s3Key").value("img-key"))
                    .andExpect(jsonPath("$.data.files").isArray())
                    .andExpect(jsonPath("$.data.files.length()").value(0));

            verify(memoService, times(1))
                    .issuePresignedUrls(eq(userId), any(MemoPresignedUrlRequest.class));
        }

        @Test
        @DisplayName("파일만 있는 경우도 성공해야 한다.")
        @WithMockCustomUser(userId = 2L)
        void issuePresignedUrls_OnlyFiles_Success() throws Exception {
            // given
            Long userId = 2L;

            MemoPresignedUrlRequest request = new MemoPresignedUrlRequest(
                    List.of(),  // 이미지 없음
                    List.of(
                            new MemoPresignedUrlRequest.UploadRequest("pdf", 5120L, 1)
                    )
            );

            MemoPresignedUrlResponse expectedResponse = new MemoPresignedUrlResponse(
                    List.of(),
                    List.of(
                            new MemoPresignedUrlResponse.PresignedUrlResponse(
                                    "file-key", "http://s3/file", 5120L, "pdf", 1)
                    )
            );

            when(memoService.issuePresignedUrls(eq(userId), any(MemoPresignedUrlRequest.class)))
                    .thenReturn(expectedResponse);

            // when & then
            mockMvc.perform(post("/api/v1/memo/presigned-urls")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.images").isArray())
                    .andExpect(jsonPath("$.data.images.length()").value(0))
                    .andExpect(jsonPath("$.data.files").isArray())
                    .andExpect(jsonPath("$.data.files.length()").value(1))
                    .andExpect(jsonPath("$.data.files[0].s3Key").value("file-key"));

            verify(memoService, times(1))
                    .issuePresignedUrls(eq(userId), any(MemoPresignedUrlRequest.class));
        }

        @Test
        @DisplayName("images가 null인 경우 실패해야 한다.")
        @WithMockCustomUser(userId = 1L)
        void issuePresignedUrls_ImagesNull_Fail() throws Exception {
            // given
            String requestJson = """
                    {
                        "images": null,
                        "files": []
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/memo/presigned-urls")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest());  // @NotNull 검증 실패

            verify(memoService, never()).issuePresignedUrls(anyLong(), any());
        }

        @Test
        @DisplayName("files가 null인 경우 실패해야 한다.")
        @WithMockCustomUser(userId = 1L)
        void issuePresignedUrls_FilesNull_Fail() throws Exception {
            // given
            String requestJson = """
                    {
                        "images": [],
                        "files": null
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/memo/presigned-urls")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(memoService, never()).issuePresignedUrls(anyLong(), any());
        }

        @Test
        @DisplayName("extension가 null인 경우 실패해야 한다.")
        @WithMockCustomUser(userId = 1L)
        void issuePresignedUrls_ExtensionNull_Fail() throws Exception {
            // given
            String requestJson = """
                    {
                        "images": [
                            {
                                "extension": null,
                                "bytes": 1024,
                                "priority": 1
                            }
                        ],
                        "files": []
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/memo/presigned-urls")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(memoService, never()).issuePresignedUrls(anyLong(), any());
        }

        @Test
        @DisplayName("bytes가 null인 경우 실패해야 한다.")
        @WithMockCustomUser(userId = 1L)
        void issuePresignedUrls_BytesNull_Fail() throws Exception {
            // given
            String requestJson = """
                    {
                        "images": [
                            {
                                "extension": "jpg",
                                "bytes": null,
                                "priority": 1
                            }
                        ],
                        "files": []
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/memo/presigned-urls")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(memoService, never()).issuePresignedUrls(anyLong(), any());
        }

        @Test
        @DisplayName("priority가 null인 경우 실패해야 한다.")
        @WithMockCustomUser(userId = 1L)
        void issuePresignedUrls_PriorityNull_Fail() throws Exception {
            // given
            String requestJson = """
                    {
                        "images": [
                            {
                                "extension": "jpg",
                                "bytes": 1024,
                                "priority": null
                            }
                        ],
                        "files": []
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/memo/presigned-urls")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(memoService, never()).issuePresignedUrls(anyLong(), any());
        }

        }

        @Nested
        @DisplayName("메모 생성 테스트")
        @WithMockCustomUser(userId = 1L)
        class CreateMemoTests{

        @DisplayName("이미지/파일/라벨 없이 메모만 작성하는 것이 성공해야한다.")
        @Test
        void createMemo_Basic_Success() throws Exception {
            // given 준비
            Long userId = 1L;

            MemoCreateRequest request = new MemoCreateRequest(
                    "SOPT 세미나",                    // title
                    "7차 세미나 내용은 ~가 중요~.",    // content
                    List.of(),
                    List.of(),
                    List.of()// files
            );

            MemoResponse expectedResponse = new MemoResponse(
                    100L,                             // memoId
                    "SOPT 세미나",                    // title
                    LocalDateTime.now()               // createdAt
            );

            when(memoService.createMemo(eq(userId), any(MemoCreateRequest.class)))
                    .thenReturn(expectedResponse);

            // when & then
            mockMvc.perform(post("/api/v1/memo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())           // 201
                    .andExpect(jsonPath("$.code").value(201))
                    .andExpect(jsonPath("$.data.memoId").value(100L))
                    .andExpect(jsonPath("$.data.title").value("SOPT 세미나"))
                    .andExpect(jsonPath("$.data.createdAt").exists());

            verify(memoService, times(1))
                    .createMemo(eq(userId), any(MemoCreateRequest.class));
        }

            @Test
            @DisplayName("라벨과 함께 메모를 작성하는 것이 성공해야 한다.")
            @WithMockCustomUser(userId = 1L)
            void createMemo_WithLabels_Success() throws Exception {
                // given
                Long userId = 1L;

                MemoCreateRequest request = new MemoCreateRequest(
                        "SOPT 세미나",
                        "7차 세미나 내용은 ~가 중요~.",
                        List.of("SOPT", "교양", "레퍼런스"),  // 라벨 3개
                        List.of(),  // 이미지 없음
                        List.of()   // 파일 없음
                );

                MemoResponse expectedResponse = new MemoResponse(
                        101L,
                        "SOPT 세미나",
                        LocalDateTime.now()
                );

                when(memoService.createMemo(eq(userId), any(MemoCreateRequest.class)))
                        .thenReturn(expectedResponse);

                // when & then
                mockMvc.perform(post("/api/v1/memo")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.code").value(201))
                        .andExpect(jsonPath("$.data.memoId").value(101L))
                        .andExpect(jsonPath("$.data.title").value("SOPT 세미나"))
                        .andExpect(jsonPath("$.data.createdAt").exists());

                verify(memoService, times(1))
                        .createMemo(eq(userId), any(MemoCreateRequest.class));
            }

            @Test
            @DisplayName("이미지와 함께 메모를 작성하는 것이 성공해야 한다.")
            @WithMockCustomUser(userId = 1L)
            void createMemo_WithImages_Success() throws Exception {
                // given
                Long userId = 1L;

                MemoCreateRequest.ImageRequest image1 = new MemoCreateRequest.ImageRequest(
                        "memo-image/1/53238404-f89d-4728-9dc0-efb2a3c7787b.png",
                        "seminar_slide.png",
                        245678L,
                        "png",
                        1
                );

                MemoCreateRequest.ImageRequest image2 = new MemoCreateRequest.ImageRequest(
                        "memo-image/1/another-uuid.jpg",
                        "photo.jpg",
                        532198L,
                        "jpg",
                        2
                );

                MemoCreateRequest request = new MemoCreateRequest(
                        "이미지가 포함된 메모",
                        "세미나 사진들입니다.",
                        List.of(),                   // 라벨 없음
                        List.of(image1, image2),     // 이미지 2개
                        List.of()                    // 파일 없음
                );

                MemoResponse expectedResponse = new MemoResponse(
                        102L,
                        "이미지가 포함된 메모",
                        LocalDateTime.now()
                );

                when(memoService.createMemo(eq(userId), any(MemoCreateRequest.class)))
                        .thenReturn(expectedResponse);

                // when & then
                mockMvc.perform(post("/api/v1/memo")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.code").value(201))
                        .andExpect(jsonPath("$.data.memoId").value(102L))
                        .andExpect(jsonPath("$.data.title").value("이미지가 포함된 메모"))
                        .andExpect(jsonPath("$.data.createdAt").exists());

                verify(memoService, times(1))
                        .createMemo(eq(userId), any(MemoCreateRequest.class));
            }

            @Test
            @DisplayName("파일과 함께 메모를 작성하는 것이 성공해야 한다.")
            @WithMockCustomUser(userId = 1L)
            void createMemo_WithFiles_Success() throws Exception {
                // given
                Long userId = 1L;

                MemoCreateRequest.FileRequest file = new MemoCreateRequest.FileRequest(
                        "memo-file/1/780fd26c-8ab7-4762-b148-b9c8c071795b.pdf",
                        "SOPT_7th_seminar.pdf",
                        1048576L,
                        "pdf",
                        1
                );

                MemoCreateRequest request = new MemoCreateRequest(
                        "파일이 포함된 메모",
                        "세미나 자료입니다.",
                        List.of(),          // 라벨 없음
                        List.of(),          // 이미지 없음
                        List.of(file)       // 파일 1개
                );

                MemoResponse expectedResponse = new MemoResponse(
                        103L,
                        "파일이 포함된 메모",
                        LocalDateTime.now()
                );

                when(memoService.createMemo(eq(userId), any(MemoCreateRequest.class)))
                        .thenReturn(expectedResponse);

                // when & then
                mockMvc.perform(post("/api/v1/memo")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.code").value(201))
                        .andExpect(jsonPath("$.data.memoId").value(103L))
                        .andExpect(jsonPath("$.data.title").value("파일이 포함된 메모"))
                        .andExpect(jsonPath("$.data.createdAt").exists());

                verify(memoService, times(1))
                        .createMemo(eq(userId), any(MemoCreateRequest.class));
            }

            @Test
            @DisplayName("라벨, 이미지, 파일이 모두 포함된 메모를 작성하는 것이 성공해야 한다.")
            @WithMockCustomUser(userId = 1L)
            void createMemo_WithAll_Success() throws Exception {
                // given
                Long userId = 1L;

                MemoCreateRequest.ImageRequest image = new MemoCreateRequest.ImageRequest(
                        "memo-image/1/uuid-image.png",
                        "slide.png",
                        245678L,
                        "png",
                        1
                );

                MemoCreateRequest.FileRequest file = new MemoCreateRequest.FileRequest(
                        "memo-file/1/uuid-file.pdf",
                        "document.pdf",
                        1048576L,
                        "pdf",
                        1
                );

                MemoCreateRequest request = new MemoCreateRequest(
                        "완전한 메모",
                        "모든 것이 포함된 메모입니다.",
                        List.of("SOPT", "교양"),    // 라벨 2개
                        List.of(image),             // 이미지 1개
                        List.of(file)               // 파일 1개
                );

                MemoResponse expectedResponse = new MemoResponse(
                        104L,
                        "완전한 메모",
                        LocalDateTime.now()
                );

                when(memoService.createMemo(eq(userId), any(MemoCreateRequest.class)))
                        .thenReturn(expectedResponse);

                // when & then
                mockMvc.perform(post("/api/v1/memo")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.code").value(201))
                        .andExpect(jsonPath("$.data.memoId").value(104L))
                        .andExpect(jsonPath("$.data.title").value("완전한 메모"))
                        .andExpect(jsonPath("$.data.createdAt").exists());

                verify(memoService, times(1))
                        .createMemo(eq(userId), any(MemoCreateRequest.class));
            }

            @Test
            @DisplayName("title이 null이면 실패해야 한다.")
            @WithMockCustomUser(userId = 1L)
            void createMemo_TitleNull_Fail() throws Exception {
                // given
                String requestJson = """
            {
                "title": null,
                "content": "내용입니다.",
                "labelNames": [],
                "images": [],
                "files": []
            }
            """;

                // when & then
                mockMvc.perform(post("/api/v1/memo")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                        .andDo(print())
                        .andExpect(status().isBadRequest());  // 400

                verify(memoService, never()).createMemo(anyLong(), any());
            }

            @Test
            @DisplayName("title이 빈 문자열이면 실패해야 한다.")
            @WithMockCustomUser(userId = 1L)
            void createMemo_TitleEmpty_Fail() throws Exception {
                // given
                String requestJson = """
            {
                "title": "",
                "content": "내용입니다.",
                "labelNames": [],
                "images": [],
                "files": []
            }
            """;

                // when & then
                mockMvc.perform(post("/api/v1/memo")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                        .andDo(print())
                        .andExpect(status().isBadRequest());  // 400

                verify(memoService, never()).createMemo(anyLong(), any());
            }


            @Test
            @DisplayName("title이 공백만 있으면 실패해야 한다.")
            @WithMockCustomUser(userId = 1L)
            void createMemo_TitleBlank_Fail() throws Exception {
                // given
                String requestJson = """
            {
                "title": "   ",
                "content": "내용입니다.",
                "labelNames": [],
                "images": [],
                "files": []
            }
            """;

                // when & then
                mockMvc.perform(post("/api/v1/memo")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                        .andDo(print())
                        .andExpect(status().isBadRequest());  // 400

                verify(memoService, never()).createMemo(anyLong(), any());
            }
        }





}
