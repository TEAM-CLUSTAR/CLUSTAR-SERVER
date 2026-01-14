package org.project.domain.memo.service.MemoServiceImplTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.domain.label.repository.LabelRepository;
import org.project.domain.memo.dto.request.MemoCreateRequest;
import org.project.domain.memo.dto.response.MemoListDashboardResponse;
import org.project.domain.memo.dto.response.MemoResponse;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.MemoFile;
import org.project.domain.memo.entity.MemoImage;
import org.project.domain.memo.repository.MemoFileRepository;
import org.project.domain.memo.repository.MemoImageRepository;
import org.project.domain.memo.repository.MemoLabelRepository;
import org.project.domain.memo.repository.MemoRepository;
import org.project.domain.memo.service.MemoServiceImpl;
import org.project.domain.user.entity.User;
import org.project.domain.user.repository.UserRepository;
import org.project.global.exception.domainException.MemoException;
import org.project.global.exception.errorcode.MemoErrorCode;
import org.project.global.util.S3KeyUtil;
import org.project.global.util.S3Util;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemoServiceImplTest")
class MemoServiceImplTest {

    @InjectMocks
    private MemoServiceImpl memoService;

    @Mock private MemoRepository memoRepository;
    @Mock private UserRepository userRepository;
    @Mock private LabelRepository labelRepository;

    @Mock private MemoImageRepository memoImageRepository;
    @Mock private MemoFileRepository memoFileRepository;
    @Mock private MemoLabelRepository memoLabelRepository;

    @Mock private S3KeyUtil s3KeyUtil;
    @Mock private S3Util s3Util;

    @Mock private ApplicationEventPublisher eventPublisher;

    // 공통 테스트 데이터
    private User user;
    private MemoCreateRequest request;

    @Nested
    @DisplayName("createMemo")
    class CreateMemo {

        @BeforeEach
        void setUp() {
            // 사용자 더미
            user = User.builder()
                    .id(1L)
                    .email("test@test.com")
                    .build();

            // 요청 DTO 더미
            request = new MemoCreateRequest(
                    "테스트 제목",
                    "테스트 내용",
                    List.of("SOPT", "TEST"),
                    List.of(), // images
                    List.of()  // files
            );

            MemoCreateRequest.ImageRequest imageRequest =
                    new MemoCreateRequest.ImageRequest(
                            "memo-image/1/test.png",
                            "test.png",
                            1024L,
                            "png",
                            1
                    );

            MemoCreateRequest.FileRequest fileRequest =
                    new MemoCreateRequest.FileRequest(
                            "memo-file/1/test.pdf",
                            "test.pdf",
                            2048L,
                            "pdf",
                            1
                    );

            request = new MemoCreateRequest(
                    "테스트 제목",
                    "테스트 내용",
                    List.of("SOPT"),
                    List.of(imageRequest),
                    List.of(fileRequest)
            );
        }

        @Test
        @DisplayName("메모 생성 성공")
        void createMemo_success() {
            // given
            when(userRepository.findById(user.getId()))
                    .thenReturn(Optional.of(user));

            when(memoRepository.save(any(Memo.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // when
            MemoResponse response = memoService.createMemo(user.getId(), request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.title()).isEqualTo("테스트 제목");

            verify(userRepository, times(1)).findById(user.getId());
            verify(memoRepository, times(1)).save(any(Memo.class));
        }

        @Test
        @DisplayName("메모 생성 실패 - 사용자가 존재하지 않음")
        void createMemo_fail_userNotFound() {
            // given
            when(userRepository.findById(user.getId()))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memoService.createMemo(user.getId(), request))
                    .isInstanceOf(RuntimeException.class);
            // 실제 예외 타입 있으면 그걸로 교체 (ex. UserException)

            verify(userRepository, times(1)).findById(user.getId());
            verify(memoRepository, never()).save(any());
        }

        @Test
        @DisplayName("메모 생성 실패 - 라벨 저장 중 예외 발생")
        void createMemo_fail_labelError() {
            // given
            when(userRepository.findById(user.getId()))
                    .thenReturn(Optional.of(user));

            when(labelRepository.findByNameAndUser(anyString(), any()))
                    .thenThrow(new RuntimeException("라벨 오류"));

            // when & then
            assertThatThrownBy(() -> memoService.createMemo(user.getId(), request))
                    .isInstanceOf(RuntimeException.class);

            verify(memoRepository, never()).save(any());
        }

        @Test
        @DisplayName("메모 생성 시 이미지와 파일 메타데이터가 함께 저장된다")
        void createMemo_withImagesAndFiles_success() {
            // given
            when(userRepository.findById(user.getId()))
                    .thenReturn(Optional.of(user));

            when(memoRepository.save(any(Memo.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // when
            MemoResponse response = memoService.createMemo(user.getId(), request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.title()).isEqualTo("테스트 제목");

            verify(memoRepository, times(1)).save(any(Memo.class));

            // 이미지 저장 검증
            verify(memoImageRepository, times(1))
                    .saveAll(anyList());

            // 파일 저장 검증
            verify(memoFileRepository, times(1))
                    .saveAll(anyList());
        }

        @Test
        @DisplayName("S3Key 검증 실패 시 예외 발생하고 트랜잭션 롤백된다")
        void createMemo_fail_whenInvalidS3Key_thenRollback() {
            // given
            Long userId = 1L;

            User user = User.builder()
                    .id(userId)
                    .email("test@test.com")
                    .build();

            MemoCreateRequest.ImageRequest imageRequest =
                    new MemoCreateRequest.ImageRequest(
                            "memo-image/999/invalid.png",
                            "invalid.png",
                            1024L,
                            "png",
                            1
                    );

            MemoCreateRequest request = new MemoCreateRequest(
                    "제목",
                    "내용",
                    List.of("SOPT"),
                    List.of(imageRequest),
                    List.of()
            );

            // 사용자 조회 OK
            when(userRepository.findById(userId))
                    .thenReturn(Optional.of(user));

            // memo save는 호출되지만
            when(memoRepository.save(any(Memo.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // S3Key 검증 실패 강제
            doThrow(new MemoException(MemoErrorCode.S3_KEY_USER_MISMATCH))
                    .when(s3KeyUtil)
                    .validateS3KeyOwner(eq(userId), anyString());

            // when & then
            assertThatThrownBy(() -> memoService.createMemo(userId, request))
                    .isInstanceOf(MemoException.class)
                    .hasMessage(MemoErrorCode.S3_KEY_USER_MISMATCH.getMsg());

            // 메모 저장은 시도됨
            verify(memoRepository, times(1)).save(any(Memo.class));

            // 이미지/파일 저장 로직은 중단됨
            verify(memoImageRepository, never()).saveAll(any());
            verify(memoFileRepository, never()).saveAll(any());
        }
    }

    @Nested
    @DisplayName("getMemosWithMedia")
    class GetMemosWithMedia {

        private User user;
        private Memo memo1;
        private Memo memo2;

        private MemoImage image1;
        private MemoFile file1;

        @BeforeEach
        void setUp() {
            user = User.builder()
                    .id(1L)
                    .email("test@test.com")
                    .build();

            memo1 = Memo.builder()
                    .id(1L)
                    .title("메모1")
                    .content("내용1")
                    .user(user)
                    .isDeleted(false)
                    .build();

            memo2 = Memo.builder()
                    .id(2L)
                    .title("메모2")
                    .content("내용2")
                    .user(user)
                    .isDeleted(false)
                    .build();

            image1 = MemoImage.builder()
                    .id(10L)
                    .memo(memo1)
                    .imageS3Key("memo-image/1/img.png")
                    .imagePriority(1)
                    .build();

            file1 = MemoFile.builder()
                    .id(20L)
                    .memo(memo1)
                    .fileS3Key("memo-file/1/file.pdf")
                    .filePriority(1)
                    .build();
        }

        @Test
        @DisplayName("성공: 메모 + 이미지 + 파일을 함께 조회한다")
        void success() {
            // given
            when(memoRepository.findMemos(
                    eq(user.getId()),
                    isNull(),
                    isNull(),
                    isNull(),
                    any(PageRequest.class)
            )).thenReturn(List.of(memo1, memo2));

            when(memoImageRepository.findByMemoIdIn(List.of(1L, 2L)))
                    .thenReturn(List.of(image1));

            when(memoFileRepository.findByMemoIdIn(List.of(1L, 2L)))
                    .thenReturn(List.of(file1));

            // when
            MemoListDashboardResponse response =
                    memoService.getMemosWithMedia(
                            user.getId(),
                            null,
                            null,
                            null,
                            10
                    );

            // then
            assertThat(response).isNotNull();
            assertThat(response.memos()).hasSize(2);

            MemoListDashboardResponse.MemoDashboardResponse memoResponse =
                    response.memos().get(0);

            assertThat(memoResponse.memoId()).isEqualTo(1L);
            assertThat(memoResponse.title()).isEqualTo("메모1");
            assertThat(memoResponse.imageCount()).isEqualTo(1);
            assertThat(memoResponse.fileCount()).isEqualTo(1);

            // repository 호출 검증
            verify(memoRepository).findMemos(
                    eq(user.getId()),
                    isNull(),
                    isNull(),
                    isNull(),
                    any(PageRequest.class)
            );

            verify(memoImageRepository).findByMemoIdIn(List.of(1L, 2L));
            verify(memoFileRepository).findByMemoIdIn(List.of(1L, 2L));
        }

        @Test
        @DisplayName("성공: 메모가 없으면 빈 응답을 반환한다")
        void success_emptyMemos() {
            // given
            when(memoRepository.findMemos(
                    eq(user.getId()),
                    isNull(),
                    isNull(),
                    isNull(),
                    any(PageRequest.class)
            )).thenReturn(List.of());

            // when
            MemoListDashboardResponse response =
                    memoService.getMemosWithMedia(
                            user.getId(),
                            null,
                            null,
                            null,
                            10
                    );

            // then
            assertThat(response).isNotNull();
            assertThat(response.memos()).isEmpty();

            // 메모만 조회되고, 미디어 조회는 안 됨
            verify(memoRepository).findMemos(
                    eq(user.getId()),
                    isNull(),
                    isNull(),
                    isNull(),
                    any(PageRequest.class)
            );
            verifyNoInteractions(memoImageRepository);
            verifyNoInteractions(memoFileRepository);
        }

        @Test
        @DisplayName("성공: 이미지만 있고 파일은 없는 경우")
        void success_onlyImages() {
            // given
            when(memoRepository.findMemos(
                    eq(user.getId()),
                    isNull(),
                    isNull(),
                    isNull(),
                    any(PageRequest.class)
            )).thenReturn(List.of(memo1));

            when(memoImageRepository.findByMemoIdIn(List.of(1L)))
                    .thenReturn(List.of(image1));

            when(memoFileRepository.findByMemoIdIn(List.of(1L)))
                    .thenReturn(List.of()); // 파일 없음

            // when
            MemoListDashboardResponse response =
                    memoService.getMemosWithMedia(
                            user.getId(),
                            null,
                            null,
                            null,
                            10
                    );

            // then
            assertThat(response).isNotNull();
            assertThat(response.memos()).hasSize(1);

            MemoListDashboardResponse.MemoDashboardResponse memoResponse =
                    response.memos().get(0);

            assertThat(memoResponse.memoId()).isEqualTo(1L);
            assertThat(memoResponse.imageCount()).isEqualTo(1);
            assertThat(memoResponse.fileCount()).isEqualTo(0);

            verify(memoRepository).findMemos(
                    eq(user.getId()),
                    isNull(),
                    isNull(),
                    isNull(),
                    any(PageRequest.class)
            );
            verify(memoImageRepository).findByMemoIdIn(List.of(1L));
            verify(memoFileRepository).findByMemoIdIn(List.of(1L));
        }

        @Test
        @DisplayName("성공: cursorCreatedAt + cursorMemoId 기준으로 다음 페이지를 조회한다")
        void success_cursorPagination() {
            // given
            LocalDateTime cursorCreatedAt =
                    LocalDateTime.of(2026, 1, 13, 11, 0);
            Long cursorMemoId = 2L;

            Memo nextMemo = Memo.builder()
                    .id(1L)
                    .title("다음 페이지 메모")
                    .content("내용")
                    .user(user)
                    .build();

            ReflectionTestUtils.setField(
                    nextMemo,
                    "createdAt",
                    LocalDateTime.of(2026, 1, 13, 10, 0)
            );

            when(memoRepository.findMemos(
                    eq(user.getId()),
                    isNull(),
                    eq(cursorCreatedAt),
                    eq(cursorMemoId),
                    any(PageRequest.class)
            )).thenReturn(List.of(nextMemo));

            when(memoImageRepository.findByMemoIdIn(List.of(1L)))
                    .thenReturn(List.of());

            when(memoFileRepository.findByMemoIdIn(List.of(1L)))
                    .thenReturn(List.of());

            // when
            MemoListDashboardResponse response =
                    memoService.getMemosWithMedia(
                            user.getId(),
                            null,
                            cursorCreatedAt,
                            cursorMemoId,
                            10
                    );

            // then
            assertThat(response).isNotNull();
            assertThat(response.memos()).hasSize(1);

            MemoListDashboardResponse.MemoDashboardResponse memoResponse =
                    response.memos().get(0);

            assertThat(memoResponse.memoId()).isEqualTo(1L);
            assertThat(memoResponse.title()).isEqualTo("다음 페이지 메모");

            verify(memoRepository).findMemos(
                    eq(user.getId()),
                    isNull(),
                    eq(cursorCreatedAt),
                    eq(cursorMemoId),
                    any(PageRequest.class)
            );
        }
    }
}
