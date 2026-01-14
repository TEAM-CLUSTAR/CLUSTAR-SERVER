package org.project.domain.memo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.domain.label.repository.LabelRepository;
import org.project.domain.memo.dto.request.MemoCreateRequest;
import org.project.domain.memo.dto.request.MemoPresignedUrlRequest;
import org.project.domain.memo.dto.response.MemoDetailResponse;
import org.project.domain.memo.dto.response.MemoListDashboardResponse;
import org.project.domain.memo.dto.response.MemoPresignedUrlResponse;
import org.project.domain.memo.dto.response.MemoResponse;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.MemoFile;
import org.project.domain.memo.entity.MemoImage;
import org.project.domain.memo.event.MemoDeletedEvent;
import org.project.domain.memo.repository.MemoFileRepository;
import org.project.domain.memo.repository.MemoImageRepository;
import org.project.domain.memo.repository.MemoLabelRepository;
import org.project.domain.memo.repository.MemoRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
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

    // 공통 테스트 데이터 상수
    private final Long userId = 1L;
    private final Long memoId = 100L;

    @Nested
    @DisplayName("createMemo")
    class CreateMemo {

        private User user;
        private MemoCreateRequest request;

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

    @Nested
    @DisplayName("메모 상세조회(모달창) 테스트")
    class getOneMemoDetail {
        @DisplayName("메모 모달창을 상세 조회할 수 있어야 한다.")
        @Test
        void getOneMemoDetail_Success() {
            // given 준비
            User user = User.builder().id(userId).build();
            Memo memo = Memo.createMemo("테스트 제목", "테스트 내용", user);
            given(memoRepository.findByIdAndNotDeleted(memoId)).willReturn(Optional.of(memo));

            // when 실행
            MemoDetailResponse response = memoService.getOneMemoDetail(userId, memoId);

            // then 검증
            assertThat(response).isNotNull();
            assertThat(response.title()).isEqualTo("테스트 제목");
            assertThat(response.content()).isEqualTo("테스트 내용");
            verify(memoRepository, times(1)).findByIdAndNotDeleted(memoId);
        }

        @DisplayName("존재하지 않는 메모리를 조회하면 MEMO_NOT_FOUND 예외가 발생한다.")
        @Test
        void getOneMemoDetail_NotFound() {
            // given: 존재하지 않는 ID 설정
            Long invalidMemoId = 999L;
            given(memoRepository.findByIdAndNotDeleted(invalidMemoId)).willReturn(Optional.empty());

            // when & then: 예외 발생 검증
            assertThatThrownBy(() -> memoService.getOneMemoDetail(userId, invalidMemoId))
                    .isInstanceOf(MemoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemoErrorCode.MEMO_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("메모 삭제 테스트")
    class deleteMemo {

        @DisplayName("메모 삭제 시 DB 데이터가 삭제되고 삭제 이벤트가 발행되어야 한다.")
        @Test
        void deleteMemo_Success() {
            // given 준비
            User user = User.builder().id(userId).build();
            Memo memo = Memo.createMemo("삭제할 메모", "내용", user);
            // 삭제 하기 전 조회가 되어야 하므로
            given(memoRepository.findByIdAndNotDeleted(memoId)).willReturn(Optional.of(memo));

            // when 실행
            memoService.deleteMemo(userId, memoId);

            // then 검증
            verify(memoImageRepository).deleteByMemo(memo);
            verify(memoFileRepository).deleteByMemo(memo);
            verify(memoLabelRepository).deleteByMemo(memo);

            verify(eventPublisher).publishEvent(any(MemoDeletedEvent.class));
        }

        @DisplayName("본인의 메모가 아닌 경우 FORBIDDEN_MEMO 예외가 발생하며 삭제되지 않는다.")
        @Test
        void deleteMemo_Forbidden_Exception() {
            // given 준비
            Long intruderId = 999L;
            User owner = User.builder().id(userId).build();
            Memo memo = Memo.createMemo("주인의 메모", "내용", owner);
            given(memoRepository.findByIdAndNotDeleted(memoId)).willReturn(Optional.of(memo));

            // when & then 실행 및 검증
            assertThatThrownBy(() -> memoService.deleteMemo(intruderId, memoId))
                    .isInstanceOf(MemoException.class) // MemoException이 발생해야 하고
                    .hasFieldOrPropertyWithValue("errorCode", MemoErrorCode.FORBIDDEN_MEMO);
            // 실제 삭제 로직 실행되지 않았음을 검증
            verify(memoImageRepository, never()).deleteByMemo(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @DisplayName("메모 삭제 시 포함된 이미지와 S3 키들이 수집되어 이벤트에 담겨야 한다.")
        @Test
        void deleteMemo_With_Media_Keys() {
            // given: 이미지와 파일이 포함된 메모 준비
            User user = User.builder().id(userId).build();
            Memo memo = Memo.createMemo("제목", "내용", user);
            String imageKey1 = "memo/images/key-001.jpg";
            String imageKey2 = "memo/images/key-002.png";
            memo.addImage(imageKey1, 1024L, "jpg", 1);
            memo.addImage(imageKey2, 2048L, "png", 2);
            given(memoRepository.findByIdAndNotDeleted(memoId)).willReturn(Optional.of(memo));

            // when
            memoService.deleteMemo(userId, memoId);

            // then: 이벤트가 발행될 때 전달된 데이터를 캡처해서 검사
            ArgumentCaptor<MemoDeletedEvent> eventCaptor = ArgumentCaptor.forClass(MemoDeletedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            MemoDeletedEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.getMemoId()).isEqualTo(memoId);
            assertThat(capturedEvent.getImageKeys())
                    .hasSize(2) // 개수가 2개여야 함
                    .containsExactlyInAnyOrder(imageKey1, imageKey2);

            assertThat(memo.getIsDeleted()).isTrue();
        }
    }

    @Nested
    @DisplayName("Presigned URL 발급 테스트")
    class issuePresignedUrls {

        @Test
        @DisplayName("이미지와 파일 확장자 정보를 보내면 S3Util을 통해 URL 목록을 발급한다.")
        void issuePresignedUrls_Success() {
            // given
            var imageUploadReq = new MemoPresignedUrlRequest.UploadRequest("jpg", 1024L, 1);
            var fileUploadReq = new MemoPresignedUrlRequest.UploadRequest("pdf", 2048L, 2);

            var request = new MemoPresignedUrlRequest(
                    List.of(imageUploadReq),
                    List.of(fileUploadReq)
            );

            // S3Util이 반환할 가짜 응답 데이터 준비
            var mockImageRes = new MemoPresignedUrlResponse.PresignedUrlResponse(
                    "memo-image/1/uuid.jpg", "https://s3.url/image", 1024L, "jpg", 1);
            var mockFileRes = new MemoPresignedUrlResponse.PresignedUrlResponse(
                    "memo-file/1/uuid.pdf", "https://s3.url/file", 2048L, "pdf", 2);

            // Mock 행동 설정
            given(s3Util.createPresignedPutUrl(eq(userId), eq("memo-image"), eq("jpg"), eq(1024L), eq(1)))
                    .willReturn(mockImageRes);
            given(s3Util.createPresignedPutUrl(eq(userId), eq("memo-file"), eq("pdf"), eq(2048L), eq(2)))
                    .willReturn(mockFileRes);

            // when 실행
            MemoPresignedUrlResponse response = memoService.issuePresignedUrls(userId, request);

            // then 검증
            assertThat(response).isNotNull();

            // 이미지 결과 검증
            assertThat(response.images()).hasSize(1);
            assertThat(response.images().get(0).presignedUrl()).isEqualTo("https://s3.url/image");

            // 파일 결과 검증
            assertThat(response.files()).hasSize(1);
            assertThat(response.files().get(0).presignedUrl()).isEqualTo("https://s3.url/file");

            // S3Util이 각각 1번씩 호출되었는지 확인
            verify(s3Util, times(1)).createPresignedPutUrl(eq(userId), eq("memo-image"), any(), any(), any());
            verify(s3Util, times(1)).createPresignedPutUrl(eq(userId), eq("memo-file"), any(), any(), any());
        }
    }
}