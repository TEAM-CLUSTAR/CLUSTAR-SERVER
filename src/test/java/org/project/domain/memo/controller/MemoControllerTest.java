package org.project.domain.memo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.project.domain.memo.dto.request.MemoCreateRequest;
import org.project.domain.memo.dto.request.MemoPresignedUrlRequest;
import org.project.domain.memo.dto.response.MemoDetailResponse;
import org.project.domain.memo.dto.response.MemoListDashboardResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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

    // 커스텀 어노테이션 -> 테스트 전 authentication 객체를 자동 설정하기 위해
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
    class CreateMemoTests {

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

        @Test
        @DisplayName("content가 null이면 실패해야 한다.")
        @WithMockCustomUser(userId = 1L)
        void createMemo_ContentNull_Fail() throws Exception {
            // given
            String requestJson = """
                    {
                        "title": "제목입니다.",
                        "content": null,
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
        @DisplayName("content가 빈 문자열이면 실패해야 한다.")
        @WithMockCustomUser(userId = 1L)
        void createMemo_ContentEmpty_Fail() throws Exception {
            // given
            String requestJson = """
                    {
                        "title": "제목입니다.",
                        "content": "",
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
        @DisplayName("content가 공백만 있으면 실패해야 한다.")
        @WithMockCustomUser(userId = 1L)
        void createMemo_ContentBlank_Fail() throws Exception {
            // given
            String requestJson = """
                    {
                        "title": "제목입니다.",
                        "content": "   ",
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
        @DisplayName("title 필드가 누락되면 실패해야 한다.")
        @WithMockCustomUser(userId = 1L)
        void createMemo_TitleMissing_Fail() throws Exception {
            // given
            String requestJson = """
                    {
                        "content": "내용입니다.",
                        "labelNames": [],
                        "images": [],
                        "files": []
                    }
                    """;
            // title 필드 자체가 없음

            // when & then
            mockMvc.perform(post("/api/v1/memo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest());  // 400

            verify(memoService, never()).createMemo(anyLong(), any());
        }

        @Test
        @DisplayName("content 필드가 누락되면 실패해야 한다.")
        @WithMockCustomUser(userId = 1L)
        void createMemo_ContentMissing_Fail() throws Exception {
            // given
            String requestJson = """
                    {
                        "title": "제목입니다.",
                        "labelNames": [],
                        "images": [],
                        "files": []
                    }
                    """;
            // content 필드 자체가 없음

            // when & then
            mockMvc.perform(post("/api/v1/memo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest());  // 400

            verify(memoService, never()).createMemo(anyLong(), any());
        }

    }

    @Nested
    @DisplayName("메모 목록 조회 테스트")
    class GetMemosTest {

        @Test
        @DisplayName("파라미터 없을 시 기본(첫) 메모 목록 조회 성공해야 한다.")
        @WithMockCustomUser(userId = 1L)
        void getMemos_Default_Success() throws Exception {
            // given
            Long userId = 1L;

            MemoListDashboardResponse.MemoDashboardResponse memo1 =
                    new MemoListDashboardResponse.MemoDashboardResponse(
                            1L,
                            "메모 제목 1",
                            "메모 내용 1",
                            "http://s3/image1.jpg",  // 대표 이미지
                            2,  // 이미지 개수
                            1,  // 파일 개수
                            false,  // isPinned
                            false,  // isAiGenerated
                            true,
                            LocalDateTime.now(),
                            List.of(
                                    new MemoListDashboardResponse.LabelResponse(1L, "SOPT", "#FF5722"),
                                    new MemoListDashboardResponse.LabelResponse(2L, "교양", "#4CAF50")
                            )
                    );

            MemoListDashboardResponse.MemoDashboardResponse memo2 =
                    new MemoListDashboardResponse.MemoDashboardResponse(
                            2L,
                            "메모 제목 2",
                            "메모 내용 2",
                            null,  // 이미지 없음
                            0,
                            0,
                            true,  // isPinned
                            false,
                            true,
                            LocalDateTime.now(),
                            List.of()  // 라벨 없음
                    );

            MemoListDashboardResponse expectedResponse =
                    new MemoListDashboardResponse(2L, List.of(memo1, memo2));

            when(memoService.getMemosWithMedia(
                    eq(userId),
                    eq(null),  // labelIds
                    eq(null),  // cursorCreatedAt
                    eq(null),  // cursorMemoId
                    eq(20)     // default size
            )).thenReturn(expectedResponse);

            // when & then
            mockMvc.perform(get("/api/v1/memo")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.memos").isArray())
                    .andExpect(jsonPath("$.data.memos.length()").value(2))
                    .andExpect(jsonPath("$.data.memos[0].memoId").value(1L))
                    .andExpect(jsonPath("$.data.memos[0].title").value("메모 제목 1"))
                    .andExpect(jsonPath("$.data.memos[0].imageCount").value(2))
                    .andExpect(jsonPath("$.data.memos[0].fileCount").value(1))
                    .andExpect(jsonPath("$.data.memos[0].isPinned").value(false))
                    .andExpect(jsonPath("$.data.memos[0].labelList.length()").value(2))
                    .andExpect(jsonPath("$.data.memos[1].memoId").value(2L))
                    .andExpect(jsonPath("$.data.memos[1].isPinned").value(true));

            verify(memoService, times(1))
                    .getMemosWithMedia(eq(userId), eq(null), eq(null), eq(null), eq(20));
        }

        @Test
        @DisplayName("단일 라벨로 필터링하여 조회가 성공해야 한다.")
        @WithMockCustomUser(userId = 1L)
        void getMemos_WithLabelFilter_Success() throws Exception {
            // given
            Long userId = 1L;
            List<Long> labelIds = List.of(1L);

            MemoListDashboardResponse.MemoDashboardResponse memo =
                    new MemoListDashboardResponse.MemoDashboardResponse(
                            1L,
                            "SOPT 관련 메모",
                            "내용",
                            null,
                            0,
                            0,
                            false,
                            false,
                            true,
                            LocalDateTime.now(),
                            List.of(
                                    new MemoListDashboardResponse.LabelResponse(1L, "SOPT", "#FF5722")
                            )
                    );

            MemoListDashboardResponse expectedResponse =
                    new MemoListDashboardResponse(1L, List.of(memo));

            when(memoService.getMemosWithMedia(
                    eq(userId),
                    eq(labelIds),
                    eq(null),
                    eq(null),
                    eq(20)
            )).thenReturn(expectedResponse);

            // when & then
            mockMvc.perform(get("/api/v1/memo")
                            .param("labelIds", "1")  // List<Long> 파라미터
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.memos.length()").value(1))
                    .andExpect(jsonPath("$.data.memos[0].labelList[0].labelId").value(1L))
                    .andExpect(jsonPath("$.data.memos[0].labelList[0].name").value("SOPT"));

            verify(memoService, times(1))
                    .getMemosWithMedia(eq(userId), eq(labelIds), eq(null), eq(null), eq(20));
        }

        @Test
        @DisplayName("무한 스크롤에 따라 커서 페이지네이션 조회가 성공해야 한다.")
        @WithMockCustomUser(userId = 1L)
        void getMemos_WithCursor_Success() throws Exception {
            // given
            Long userId = 1L;
            LocalDateTime cursorCreatedAt = LocalDateTime.of(2026, 1, 15, 12, 0);
            Long cursorMemoId = 10L;
            int size = 10;

            MemoListDashboardResponse expectedResponse =
                    new MemoListDashboardResponse(0L, List.of());

            when(memoService.getMemosWithMedia(
                    eq(userId),
                    eq(null),
                    eq(cursorCreatedAt),
                    eq(cursorMemoId),
                    eq(size)
            )).thenReturn(expectedResponse);

            // when & then
            mockMvc.perform(get("/api/v1/memo")
                            .param("cursorCreatedAt", "2026-01-15T12:00:00")
                            .param("cursorMemoId", "10")
                            .param("size", "10")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.memos").isArray());

            verify(memoService, times(1))
                    .getMemosWithMedia(eq(userId), eq(null), any(LocalDateTime.class), eq(cursorMemoId), eq(size));
        }

        @Test
        @DisplayName("size 파라미터 지정하여 조회에 성공하여야 한다.")
        @WithMockCustomUser(userId = 1L)
        void getMemos_WithCustomSize_Success() throws Exception {
            // given
            Long userId = 1L;
            int customSize = 5;

            MemoListDashboardResponse expectedResponse =
                    new MemoListDashboardResponse(0L, List.of());

            when(memoService.getMemosWithMedia(
                    eq(userId),
                    eq(null),
                    eq(null),
                    eq(null),
                    eq(customSize)
            )).thenReturn(expectedResponse);

            // when & then
            mockMvc.perform(get("/api/v1/memo")
                            .param("size", "5")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk());

            verify(memoService, times(1))
                    .getMemosWithMedia(eq(userId), eq(null), eq(null), eq(null), eq(customSize));
        }

        @Test
        @DisplayName("메모 목록이 더 이상 없을 때 빈 목록 반환에 성공해야 한다.")
        @WithMockCustomUser(userId = 1L)
        void getMemos_EmptyList_Success() throws Exception {
            // given
            Long userId = 1L;

            MemoListDashboardResponse expectedResponse =
                    new MemoListDashboardResponse(0L, List.of());  // 빈 리스트

            when(memoService.getMemosWithMedia(
                    eq(userId),
                    eq(null),
                    eq(null),
                    eq(null),
                    eq(20)
            )).thenReturn(expectedResponse);

            // when & then
            mockMvc.perform(get("/api/v1/memo")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.memos").isArray())
                    .andExpect(jsonPath("$.data.memos.length()").value(0));

            verify(memoService, times(1))
                    .getMemosWithMedia(eq(userId), eq(null), eq(null), eq(null), eq(20));
        }
    }

    @Nested
    @DisplayName("메모 상세 조회 테스트")
    class GetOneDetailMemoTest {

        @Test
        @DisplayName("이미지/파일/라벨이 모두 포함된 메모 상세 조회가 성공해야 한다.")
        @WithMockCustomUser(userId = 1L)
        void getOneDetailMemo_WithAll_Success() throws Exception {
            // given
            Long userId = 1L;
            Long memoId = 100L;

            MemoDetailResponse.ImageInfo image1 = new MemoDetailResponse.ImageInfo(
                    1L,
                    "http://s3/presigned/image1.jpg",
                    "seminar_slide.png",
                    "png",
                    "0.24MB"
            );

            MemoDetailResponse.ImageInfo image2 = new MemoDetailResponse.ImageInfo(
                    2L,
                    "http://s3/presigned/image2.jpg",
                    "photo.jpg",
                    "jpg",
                    "1.50MB"
            );

            MemoDetailResponse.FileInfo file = new MemoDetailResponse.FileInfo(
                    1L,
                    "http://s3/presigned/file1.pdf",
                    "SOPT_7th_seminar.pdf",
                    "pdf",
                    "1.00MB"
            );

            MemoDetailResponse expectedResponse = new MemoDetailResponse(
                    memoId,
                    "SOPT 세미나 정리",
                    "7차 세미나 내용은 매우 중요합니다.",
                    List.of(image1, image2),
                    List.of(file),
                    List.of(
                            new MemoListDashboardResponse.LabelResponse(1L, "SOPT", "#FF5722"),
                            new MemoListDashboardResponse.LabelResponse(2L, "교양", "#4CAF50"),
                            new MemoListDashboardResponse.LabelResponse(3L, "레퍼런스", "#2196F3")
                    ),
                    LocalDateTime.of(2026, 1, 16, 10, 30),
                    false,  // AI 생성 아님
                    List.of()  // sourceList 비어있음
            );

            when(memoService.getOneMemoDetail(eq(userId), eq(memoId)))
                    .thenReturn(expectedResponse);

            // when & then
            mockMvc.perform(get("/api/v1/memo/{memoId}", memoId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.memoId").value(memoId))
                    .andExpect(jsonPath("$.data.title").value("SOPT 세미나 정리"))
                    .andExpect(jsonPath("$.data.content").value("7차 세미나 내용은 매우 중요합니다."))
                    .andExpect(jsonPath("$.data.images").isArray())
                    .andExpect(jsonPath("$.data.images.length()").value(2))
                    .andExpect(jsonPath("$.data.images[0].imageId").value(1L))
                    .andExpect(jsonPath("$.data.images[0].imageName").value("seminar_slide.png"))
                    .andExpect(jsonPath("$.data.images[0].imageSize").value("0.24MB"))
                    .andExpect(jsonPath("$.data.files").isArray())
                    .andExpect(jsonPath("$.data.files.length()").value(1))
                    .andExpect(jsonPath("$.data.files[0].fileId").value(1L))
                    .andExpect(jsonPath("$.data.files[0].fileName").value("SOPT_7th_seminar.pdf"))
                    .andExpect(jsonPath("$.data.labelList").isArray())
                    .andExpect(jsonPath("$.data.labelList.length()").value(3))
                    .andExpect(jsonPath("$.data.labelList[0].labelId").value(1L))
                    .andExpect(jsonPath("$.data.labelList[0].name").value("SOPT"))
                    .andExpect(jsonPath("$.data.isAiGenerated").value(false))
                    .andExpect(jsonPath("$.data.sourceMemoTitleList").isArray())
                    .andExpect(jsonPath("$.data.sourceMemoTitleList.length()").value(0));

            verify(memoService, times(1))
                    .getOneMemoDetail(eq(userId), eq(memoId));
        }

        @Test
        @DisplayName("sourceList가 비어있는 일반 메모 상세 조회가 성공해야 한다.")
        @WithMockCustomUser(userId = 1L)
        void getOneDetailMemo_NotAiGenerated_Success() throws Exception {
            // given
            Long userId = 1L;
            Long memoId = 300L;

            MemoDetailResponse expectedResponse = new MemoDetailResponse(
                    memoId,
                    "일반 메모",
                    "사용자가 직접 작성한 메모입니다.",
                    List.of(),
                    List.of(),
                    List.of(new MemoListDashboardResponse.LabelResponse(1L, "개인", "#9C27B0")),
                    LocalDateTime.of(2026, 1, 16, 12, 0),
                    false,  // AI 생성 아님
                    List.of()  // sourceList 비어있음
            );

            when(memoService.getOneMemoDetail(eq(userId), eq(memoId)))
                    .thenReturn(expectedResponse);

            // when & then
            mockMvc.perform(get("/api/v1/memo/{memoId}", memoId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isAiGenerated").value(false))
                    .andExpect(jsonPath("$.data.sourceMemoTitleList").isArray())
                    .andExpect(jsonPath("$.data.sourceMemoTitleList.length()").value(0));

            verify(memoService, times(1))
                    .getOneMemoDetail(eq(userId), eq(memoId));
        }

        @Test
        @DisplayName("텍스트만 있는 메모 상세 조회가 성공해야 한다.")
        @WithMockCustomUser(userId = 1L)
        void getOneDetailMemo_WithoutMedia_Success() throws Exception {
            // given
            Long userId = 1L;
            Long memoId = 400L;

            MemoDetailResponse expectedResponse = new MemoDetailResponse(
                    memoId,
                    "텍스트만 있는 메모",
                    "이미지나 파일 없이 텍스트만 있습니다.",
                    List.of(),  // 이미지 없음
                    List.of(),  // 파일 없음
                    List.of(new MemoListDashboardResponse.LabelResponse(1L, "메모", "#795548")),
                    LocalDateTime.of(2026, 1, 16, 13, 0),
                    false,
                    List.of()
            );

            when(memoService.getOneMemoDetail(eq(userId), eq(memoId)))
                    .thenReturn(expectedResponse);

            // when & then
            mockMvc.perform(get("/api/v1/memo/{memoId}", memoId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.memoId").value(memoId))
                    .andExpect(jsonPath("$.data.images").isArray())
                    .andExpect(jsonPath("$.data.images.length()").value(0))
                    .andExpect(jsonPath("$.data.files").isArray())
                    .andExpect(jsonPath("$.data.files.length()").value(0));

            verify(memoService, times(1))
                    .getOneMemoDetail(eq(userId), eq(memoId));
        }
    }

    @Nested
    @DisplayName("메모 삭제 테스트")
    class DeleteMemoTest {

        @Test
        @DisplayName("메모 삭제가 성공해야 한다.")
        @WithMockCustomUser(userId = 1L)
        void deleteMemo_Success() throws Exception {
            // given
            Long userId = 1L;
            Long memoId = 100L;

            // Service는 void 반환이므로 doNothing() 사용
            doNothing().when(memoService).deleteMemo(eq(userId), eq(memoId));

            // when & then
            mockMvc.perform(delete("/api/v1/memo/{memoId}", memoId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").doesNotExist());  // null

            verify(memoService, times(1))
                    .deleteMemo(eq(userId), eq(memoId));
        }
    }
}
