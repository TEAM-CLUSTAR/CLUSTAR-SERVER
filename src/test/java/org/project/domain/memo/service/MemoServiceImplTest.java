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
import org.project.domain.ai.rag.E.retrieve.search.MemoSearchVectorRetriever;
import org.project.domain.tag.repository.TagRepository;
import org.project.domain.memo.config.MemoRecommendationProperties;
import org.project.domain.memo.config.MemoSearchProperties;
import org.project.domain.memo.dto.request.MemoCreateRequest;
import org.project.domain.memo.dto.request.MemoPresignedUrlRequest;
import org.project.domain.memo.dto.request.MemoRecommendationRequest;
import org.project.domain.memo.dto.request.MemoUpdateRequest;
import org.project.domain.memo.dto.response.MemoDetailResponse;
import org.project.domain.memo.dto.response.MemoListDashboardResponse;
import org.project.domain.memo.dto.response.MemoPresignedUrlResponse;
import org.project.domain.memo.dto.response.MemoRecentViewedResponse;
import org.project.domain.memo.dto.response.RecentViewedSource;
import org.project.domain.memo.dto.response.MemoRecommendationResponse;
import org.project.domain.memo.dto.response.MemoResponse;
import org.project.domain.memo.dto.response.MemoSearchResponse;
import org.project.domain.memo.dto.response.SearchType;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.MemoFile;
import org.project.domain.memo.entity.MemoImage;
import org.project.domain.memo.event.MemoAttachmentsRemovedEvent;
import org.project.domain.memo.event.MemoDeletedEvent;
import org.project.domain.memo.event.MemoImageCreatedEvent;
import org.project.domain.memo.event.MemoTextUpdatedEvent;
import org.project.domain.tag.entity.Tag;
import org.project.domain.memo.repository.MemoFileRepository;
import org.project.domain.memo.repository.MemoImageRepository;
import org.project.domain.memo.repository.MemoTagRepository;
import org.project.domain.memo.repository.MemoRepository;
import org.project.domain.memo.repository.VectorStoreRepository;
import org.project.domain.user.entity.User;
import org.project.domain.user.repository.UserRepository;
import org.project.global.exception.domainException.MemoException;
import org.project.global.exception.errorcode.MemoErrorCode;
import org.project.global.util.S3KeyUtil;
import org.project.global.util.S3Util;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.Executor;

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
    @Mock private TagRepository tagRepository;

    @Mock private MemoImageRepository memoImageRepository;
    @Mock private MemoFileRepository memoFileRepository;
    @Mock private MemoTagRepository memoTagRepository;

    @Mock private S3KeyUtil s3KeyUtil;
    @Mock private S3Util s3Util;
    @Mock private TransactionTemplate transactionTemplate;

    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private MemoSearchVectorRetriever memoSearchVectorRetriever;
    @Mock private VectorStoreRepository vectorStoreRepository;
    @Mock private MemoRecommendationProperties memoRecommendationProperties;
    @Mock private MemoSearchProperties memoSearchProperties;

    // 공통 테스트 데이터 상수
    private final Long userId = 1L;
    private final Long memoId = 100L;

    // 쓰기 경로는 트랜잭션 밖에서 검증한 뒤 TransactionTemplate으로 감싸므로,
    // 단위 테스트에서는 콜백을 그 자리에서 실행시켜 기존과 같은 흐름으로 검증한다.
    @BeforeEach
    void runTransactionTemplateInline() {
        lenient().when(transactionTemplate.execute(any()))
                .thenAnswer(invocation -> ((TransactionCallback<?>) invocation.getArgument(0))
                        .doInTransaction(null));
    }

    @Nested
    @DisplayName("createMemo")
    class CreateMemo {

        private User user;
        private MemoCreateRequest request;

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(memoService, "ioExecutor", (Executor) Runnable::run);
            lenient().when(s3Util.getObjectMetadata(anyString()))
                    .thenReturn(new S3Util.S3ObjectMetadata(1_024L, "application/octet-stream"));

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
                            1
                    );

            MemoCreateRequest.FileRequest fileRequest =
                    new MemoCreateRequest.FileRequest(
                            "memo-file/1/test.pdf",
                            "test.pdf",
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
        @DisplayName("이미지 개수가 5개를 초과하면 예외가 발생한다.")
        void createMemo_TooManyImages() {
            List<MemoCreateRequest.ImageRequest> images = List.of(
                    new MemoCreateRequest.ImageRequest("memo-image/1/1.png", "1.png", 1),
                    new MemoCreateRequest.ImageRequest("memo-image/1/2.png", "2.png", 2),
                    new MemoCreateRequest.ImageRequest("memo-image/1/3.png", "3.png", 3),
                    new MemoCreateRequest.ImageRequest("memo-image/1/4.png", "4.png", 4),
                    new MemoCreateRequest.ImageRequest("memo-image/1/5.png", "5.png", 5),
                    new MemoCreateRequest.ImageRequest("memo-image/1/6.png", "6.png", 6)
            );

            MemoCreateRequest tooManyImagesRequest = new MemoCreateRequest(
                    "제목",
                    "내용",
                    null,
                    images,
                    List.of()
            );

            assertThatThrownBy(() -> memoService.createMemo(user.getId(), tooManyImagesRequest))
                    .isInstanceOf(MemoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemoErrorCode.TOO_MANY_IMAGES);
        }

        @Test
        @DisplayName("S3 실제 파일 용량이 제한을 초과하면 예외가 발생한다.")
        void createMemo_FileTooLarge() {
            List<MemoCreateRequest.FileRequest> files = List.of(
                    new MemoCreateRequest.FileRequest("memo-file/1/1.pdf", "1.pdf", 1)
            );
            given(s3Util.getObjectMetadata("memo-file/1/1.pdf"))
                    .willReturn(new S3Util.S3ObjectMetadata(10L * 1024 * 1024 + 1, "application/pdf"));

            MemoCreateRequest tooLargeFileRequest = new MemoCreateRequest(
                    "제목",
                    "내용",
                    null,
                    List.of(),
                    files
            );

            assertThatThrownBy(() -> memoService.createMemo(user.getId(), tooLargeFileRequest))
                    .isInstanceOf(MemoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemoErrorCode.FILE_TOO_LARGE);
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
        @DisplayName("메모 생성 실패 - 태그 저장 중 예외 발생")
        void createMemo_fail_tagError() {
            // given
            when(userRepository.findById(user.getId()))
                    .thenReturn(Optional.of(user));

            when(tagRepository.findAllByNameInAndUser(anyList(), any()))
                    .thenThrow(new RuntimeException("태그 오류"));

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
                            1
                    );

            MemoCreateRequest request = new MemoCreateRequest(
                    "제목",
                    "내용",
                    List.of("SOPT"),
                    List.of(imageRequest),
                    List.of()
            );

            // S3Key 검증 실패 강제
            doThrow(new MemoException(MemoErrorCode.S3_KEY_USER_MISMATCH))
                    .when(s3KeyUtil)
                    .validateS3Key(eq(userId), eq("memo-image"), anyString());

            // when & then
            assertThatThrownBy(() -> memoService.createMemo(userId, request))
                    .isInstanceOf(MemoException.class)
                    .hasMessage(MemoErrorCode.S3_KEY_USER_MISMATCH.getMsg());

            // S3 검증이 선행되므로 메모는 저장되지 않음
            verify(memoRepository, never()).save(any());

            // 이미지/파일 저장 로직은 중단됨
            verify(memoImageRepository, never()).saveAll(any());
            verify(memoFileRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("S3 실제 객체 크기를 조회해 저장한다")
        void createMemo_savesActualS3ObjectSize() {
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(memoRepository.save(any(Memo.class))).thenAnswer(invocation -> invocation.getArgument(0));
            given(s3Util.getObjectMetadata("memo-image/1/test.png"))
                    .willReturn(new S3Util.S3ObjectMetadata(3_000L, "image/png"));
            given(s3Util.getObjectMetadata("memo-file/1/test.pdf"))
                    .willReturn(new S3Util.S3ObjectMetadata(4_000L, "application/pdf"));

            memoService.createMemo(user.getId(), request);

            ArgumentCaptor<List<MemoImage>> imageCaptor = ArgumentCaptor.forClass(List.class);
            ArgumentCaptor<List<MemoFile>> fileCaptor = ArgumentCaptor.forClass(List.class);
            verify(memoImageRepository).saveAll(imageCaptor.capture());
            verify(memoFileRepository).saveAll(fileCaptor.capture());

            assertThat(imageCaptor.getValue().get(0).getImageBytes()).isEqualTo(3_000L);
            assertThat(fileCaptor.getValue().get(0).getFileBytes()).isEqualTo(4_000L);
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

            when(memoImageRepository.findRepresentativeImageS3Keys(List.of(1L, 2L)))
                    .thenReturn(Map.of(1L, image1.getImageS3Key()));
            when(memoImageRepository.countImagesByMemoId(List.of(1L, 2L)))
                    .thenReturn(Map.of(1L, 1L));

            when(memoFileRepository.countFilesByMemoId(List.of(1L, 2L)))
                    .thenReturn(Map.of(1L, 1L));

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

            verify(memoImageRepository).findRepresentativeImageS3Keys(List.of(1L, 2L));
            verify(memoImageRepository).countImagesByMemoId(List.of(1L, 2L));
            verify(memoFileRepository).countFilesByMemoId(List.of(1L, 2L));
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

            when(memoImageRepository.findRepresentativeImageS3Keys(List.of(1L)))
                    .thenReturn(Map.of(1L, image1.getImageS3Key()));
            when(memoImageRepository.countImagesByMemoId(List.of(1L)))
                    .thenReturn(Map.of(1L, 1L));

            when(memoFileRepository.countFilesByMemoId(List.of(1L)))
                    .thenReturn(Map.of()); // 파일 없음

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
            verify(memoImageRepository).findRepresentativeImageS3Keys(List.of(1L));
            verify(memoImageRepository).countImagesByMemoId(List.of(1L));
            verify(memoFileRepository).countFilesByMemoId(List.of(1L));
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

            when(memoImageRepository.findRepresentativeImageS3Keys(List.of(1L)))
                    .thenReturn(Map.of());
            when(memoImageRepository.countImagesByMemoId(List.of(1L)))
                    .thenReturn(Map.of());

            when(memoFileRepository.countFilesByMemoId(List.of(1L)))
                    .thenReturn(Map.of());

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
    @DisplayName("메모 상세조회 테스트")
    class getOneMemoDetail {
        @DisplayName("메모를 상세 조회할 수 있어야 한다.")
        @Test
        void getOneMemoDetail_Success() {
            // given 준비
            User user = User.builder().id(userId).build();
            Memo memo = Memo.createMemo("테스트 제목", "테스트 내용", user);
            LocalDateTime updatedAt = LocalDateTime.of(2026, 8, 24, 10, 30);
            ReflectionTestUtils.setField(memo, "updatedAt", updatedAt);
            given(memoRepository.findByIdAndNotDeleted(memoId)).willReturn(Optional.of(memo));

            // when 실행
            MemoDetailResponse response = memoService.getOneMemoDetail(userId, memoId);

            // then 검증
            assertThat(response).isNotNull();
            assertThat(response.title()).isEqualTo("테스트 제목");
            assertThat(response.content()).isEqualTo("테스트 내용");
            assertThat(response.updatedAt()).isEqualTo(updatedAt);
            verify(memoRepository, times(1)).findByIdAndNotDeleted(memoId);
        }

        @DisplayName("상세조회는 열람 기록을 touchViewed 쿼리로 남긴다 (updatedAt 오염 방지)")
        @Test
        void getOneMemoDetail_recordsViewViaTouchViewed() {
            // given
            User user = User.builder().id(userId).build();
            Memo memo = Memo.createMemo("제목", "내용", user);
            given(memoRepository.findByIdAndNotDeleted(memoId)).willReturn(Optional.of(memo));

            // when
            memoService.getOneMemoDetail(userId, memoId);

            // then: 엔티티를 dirty하게 만들지 않고 전용 쿼리로 열람 시각을 남긴다
            verify(memoRepository).touchViewed(eq(memoId), any(LocalDateTime.class));
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
            verify(memoTagRepository).deleteByMemo(memo);

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
            memo.getMemoImages().add(MemoImage.builder()
                    .memo(memo)
                    .imageS3Key(imageKey1)
                    .imageBytes(1024L)
                    .imageExtension("jpg")
                    .imagePriority(1)
                    .build());
            memo.getMemoImages().add(MemoImage.builder()
                    .memo(memo)
                    .imageS3Key(imageKey2)
                    .imageBytes(2048L)
                    .imageExtension("png")
                    .imagePriority(2)
                    .build());
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
        @DisplayName("이미지 확장자가 허용 목록에 없으면 UNSUPPORTED_EXTENSION 예외가 발생한다")
        void issuePresignedUrls_UnsupportedImageExtension_throws() {
            // given: 확장자는 그대로 s3Key에 박히고 저장 시 다시 읽히므로 발급 시점에 막는다
            var request = new MemoPresignedUrlRequest(
                    List.of(new MemoPresignedUrlRequest.UploadRequest("exe", 1024L, 1)),
                    List.of()
            );

            // when & then
            assertThatThrownBy(() -> memoService.issuePresignedUrls(userId, request))
                    .isInstanceOf(MemoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemoErrorCode.UNSUPPORTED_EXTENSION);

            verify(s3Util, never()).createPresignedPutUrl(anyLong(), anyString(), anyString(), anyLong(), anyInt());
        }

        @Test
        @DisplayName("파일 확장자가 허용 목록에 없으면 UNSUPPORTED_EXTENSION 예외가 발생한다")
        void issuePresignedUrls_UnsupportedFileExtension_throws() {
            // given
            var request = new MemoPresignedUrlRequest(
                    List.of(),
                    List.of(new MemoPresignedUrlRequest.UploadRequest("sh", 1024L, 1))
            );

            // when & then
            assertThatThrownBy(() -> memoService.issuePresignedUrls(userId, request))
                    .isInstanceOf(MemoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemoErrorCode.UNSUPPORTED_EXTENSION);
        }

        @Test
        @DisplayName("hwp 등 문서 확장자는 허용한다")
        void issuePresignedUrls_DocumentExtensions_allowed() {
            // given: Tika가 파싱하지 못하더라도 첨부 자체는 허용한다
            var request = new MemoPresignedUrlRequest(
                    List.of(),
                    List.of(new MemoPresignedUrlRequest.UploadRequest("hwp", 1024L, 1))
            );
            given(s3Util.createPresignedPutUrl(eq(userId), eq("memo-file"), eq("hwp"), eq(1024L), eq(1)))
                    .willReturn(new MemoPresignedUrlResponse.PresignedUrlResponse(
                            "memo-file/1/uuid.hwp", "https://s3.url/file", "application/octet-stream", 1024L, "hwp", 1));

            // when & then
            assertThat(memoService.issuePresignedUrls(userId, request).files()).hasSize(1);
        }

        @Test
        @DisplayName("확장자 대소문자는 구분하지 않는다 (PNG 허용)")
        void issuePresignedUrls_UpperCaseExtension_allowed() {
            // given
            var request = new MemoPresignedUrlRequest(
                    List.of(new MemoPresignedUrlRequest.UploadRequest("PNG", 1024L, 1)),
                    List.of()
            );
            given(s3Util.createPresignedPutUrl(eq(userId), eq("memo-image"), eq("PNG"), eq(1024L), eq(1)))
                    .willReturn(new MemoPresignedUrlResponse.PresignedUrlResponse(
                            "memo-image/1/uuid.PNG", "https://s3.url/image", "image/png", 1024L, "PNG", 1));

            // when & then
            assertThat(memoService.issuePresignedUrls(userId, request).images()).hasSize(1);
        }

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
                    "memo-image/1/uuid.jpg", "https://s3.url/image", "image/jpeg", 1024L, "jpg", 1);
            var mockFileRes = new MemoPresignedUrlResponse.PresignedUrlResponse(
                    "memo-file/1/uuid.pdf", "https://s3.url/file", "application/pdf", 2048L, "pdf", 2);

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

        @Test
        @DisplayName("이미지 개수가 5개를 초과하면 예외가 발생한다.")
        void issuePresignedUrls_TooManyImages() {
            List<MemoPresignedUrlRequest.UploadRequest> images = List.of(
                    new MemoPresignedUrlRequest.UploadRequest("jpg", 100L, 1),
                    new MemoPresignedUrlRequest.UploadRequest("jpg", 100L, 2),
                    new MemoPresignedUrlRequest.UploadRequest("jpg", 100L, 3),
                    new MemoPresignedUrlRequest.UploadRequest("jpg", 100L, 4),
                    new MemoPresignedUrlRequest.UploadRequest("jpg", 100L, 5),
                    new MemoPresignedUrlRequest.UploadRequest("jpg", 100L, 6)
            );

            var request = new MemoPresignedUrlRequest(images, List.of());

            assertThatThrownBy(() -> memoService.issuePresignedUrls(userId, request))
                    .isInstanceOf(MemoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemoErrorCode.TOO_MANY_IMAGES);
        }

        @Test
        @DisplayName("파일 개수가 5개를 초과하면 예외가 발생한다.")
        void issuePresignedUrls_TooManyFiles() {
            List<MemoPresignedUrlRequest.UploadRequest> files = List.of(
                    new MemoPresignedUrlRequest.UploadRequest("pdf", 100L, 1),
                    new MemoPresignedUrlRequest.UploadRequest("pdf", 100L, 2),
                    new MemoPresignedUrlRequest.UploadRequest("pdf", 100L, 3),
                    new MemoPresignedUrlRequest.UploadRequest("pdf", 100L, 4),
                    new MemoPresignedUrlRequest.UploadRequest("pdf", 100L, 5),
                    new MemoPresignedUrlRequest.UploadRequest("pdf", 100L, 6)
            );

            var request = new MemoPresignedUrlRequest(List.of(), files);

            assertThatThrownBy(() -> memoService.issuePresignedUrls(userId, request))
                    .isInstanceOf(MemoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemoErrorCode.TOO_MANY_FILES);
        }

        @Test
        @DisplayName("이미지 용량이 제한을 초과하면 예외가 발생한다.")
        void issuePresignedUrls_ImageTooLarge() {
            long overLimit = 5L * 1024 * 1024 + 1;
            var imageUploadReq = new MemoPresignedUrlRequest.UploadRequest("png", overLimit, 1);
            var request = new MemoPresignedUrlRequest(List.of(imageUploadReq), List.of());

            assertThatThrownBy(() -> memoService.issuePresignedUrls(userId, request))
                    .isInstanceOf(MemoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemoErrorCode.IMAGE_TOO_LARGE);
        }

        @Test
        @DisplayName("파일 용량이 제한을 초과하면 예외가 발생한다.")
        void issuePresignedUrls_FileTooLarge() {
            long overLimit = 10L * 1024 * 1024 + 1;
            var fileUploadReq = new MemoPresignedUrlRequest.UploadRequest("pdf", overLimit, 1);
            var request = new MemoPresignedUrlRequest(List.of(), List.of(fileUploadReq));

            assertThatThrownBy(() -> memoService.issuePresignedUrls(userId, request))
                    .isInstanceOf(MemoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemoErrorCode.FILE_TOO_LARGE);
        }
    }

    @Nested
    @DisplayName("메모 검색 테스트")
    class SearchMemos {

        @Test
        @DisplayName("빈 검색어이면 EMPTY_SEARCH_QUERY 예외가 발생한다")
        void searchMemos_emptyQuery_throwsException() {
            assertThatThrownBy(() -> memoService.searchMemos(userId, "  "))
                    .isInstanceOf(MemoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemoErrorCode.EMPTY_SEARCH_QUERY);
        }

        @Test
        @DisplayName("키워드 검색 결과를 모두 TEXT 타입으로 반환한다 (의미 검색 비활성화)")
        void searchMemos_returnsTextResultsOnly() {
            // given
            User user = User.builder().id(userId).build();
            Memo m1 = Memo.builder().id(1L).title("스프링 정리").content("내용").user(user).isDeleted(false).build();
            Memo m2 = Memo.builder().id(2L).title("스프링 부트").content("내용").user(user).isDeleted(false).build();

            given(memoRepository.searchByText(eq(userId), eq("스프링")))
                    .willReturn(List.of(m1, m2));

            // when
            MemoSearchResponse response = memoService.searchMemos(userId, "스프링");

            // then: 리포지토리가 정렬해준 순서 그대로, 전부 TEXT 타입
            assertThat(response.results()).extracting("memoId").containsExactly(1L, 2L);
            assertThat(response.results()).extracting("searchType")
                    .containsOnly(SearchType.TEXT);
        }

        @Test
        @DisplayName("검색 결과가 없으면 빈 리스트와 안내 메시지를 반환한다")
        void searchMemos_noResults_returnsEmptyWithMessage() {
            // given
            given(memoRepository.searchByText(eq(userId), eq("없는내용")))
                    .willReturn(List.of());

            // when
            MemoSearchResponse response = memoService.searchMemos(userId, "없는내용");

            // then
            assertThat(response.results()).isEmpty();
            assertThat(response.message()).isEqualTo("검색 결과가 없어요.");
        }
    }

    @Nested
    @DisplayName("최근 열람 메모 테스트")
    class RecentViewedMemos {

        @Test
        @DisplayName("최근 열람한 메모가 있으면 폴백 없이 열람 메모를 반환한다")
        void getRecentViewedMemos_returnsItems() {
            // given
            User user = User.builder().id(userId).build();
            Memo m1 = Memo.builder().id(1L).title("최근1").content("내용").user(user)
                    .isDeleted(false).lastViewedAt(LocalDateTime.now()).build();
            Memo m2 = Memo.builder().id(2L).title("최근2").content("내용").user(user)
                    .isDeleted(false).lastViewedAt(LocalDateTime.now().minusMinutes(1)).build();

            given(memoSearchProperties.getRecentViewedLimit()).willReturn(6);
            given(memoRepository.findRecentViewed(eq(userId), eq(6)))
                    .willReturn(List.of(m1, m2));

            // when
            MemoRecentViewedResponse response = memoService.getRecentViewedMemos(userId);

            // then
            assertThat(response.source()).isEqualTo(RecentViewedSource.RECENT_VIEWED);
            assertThat(response.results()).extracting("memoId").containsExactly(1L, 2L);
            assertThat(response.results()).allMatch(item -> item.lastViewedAt() != null);
            // 열람 이력이 있으므로 최근 생성 폴백은 조회하지 않는다
            verify(memoRepository, never()).findRecentCreated(anyLong(), anyInt());
        }

        @Test
        @DisplayName("열람 이력이 없으면 최근 생성 메모로 폴백한다 (lastViewedAt=null, createdAt 채움)")
        void getRecentViewedMemos_fallbackToRecentCreated() {
            // given
            User user = User.builder().id(userId).build();
            LocalDateTime created = LocalDateTime.now().minusDays(1);
            Memo created1 = Memo.builder().id(10L).title("생성1").content("내용").user(user)
                    .isDeleted(false).build(); // lastViewedAt == null (한 번도 열람 안 함)
            ReflectionTestUtils.setField(created1, "createdAt", created);

            given(memoSearchProperties.getRecentViewedLimit()).willReturn(6);
            given(memoRepository.findRecentViewed(eq(userId), eq(6)))
                    .willReturn(List.of());
            given(memoRepository.findRecentCreated(eq(userId), eq(6)))
                    .willReturn(List.of(created1));

            // when
            MemoRecentViewedResponse response = memoService.getRecentViewedMemos(userId);

            // then
            assertThat(response.source()).isEqualTo(RecentViewedSource.RECENT_CREATED);
            assertThat(response.results()).hasSize(1);
            MemoRecentViewedResponse.Item item = response.results().get(0);
            assertThat(item.memoId()).isEqualTo(10L);
            assertThat(item.lastViewedAt()).isNull();
            assertThat(item.createdAt()).isEqualTo(created);
        }

        @Test
        @DisplayName("열람 이력도 생성 메모도 없으면 빈 리스트를 반환한다")
        void getRecentViewedMemos_empty() {
            // given
            given(memoSearchProperties.getRecentViewedLimit()).willReturn(6);
            given(memoRepository.findRecentViewed(eq(userId), eq(6)))
                    .willReturn(List.of());
            given(memoRepository.findRecentCreated(eq(userId), eq(6)))
                    .willReturn(List.of());

            // when
            MemoRecentViewedResponse response = memoService.getRecentViewedMemos(userId);

            // then — 열람 이력이 없어 생성 폴백을 시도했으므로 source는 RECENT_CREATED
            assertThat(response.source()).isEqualTo(RecentViewedSource.RECENT_CREATED);
            assertThat(response.results()).isEmpty();
        }
    }

    @Nested
    @DisplayName("메모 추천 테스트")
    class RecommendMemos {

        private User user;

        @BeforeEach
        void setUp() {
            user = User.builder().id(userId).build();
            // 기본: 선택한 메모는 모두 본인 소유로 간주(소유검증 통과). 개별 테스트에서 필요 시 재정의.
            lenient().when(memoRepository.countByIdInAndUserIdAndNotDeleted(eq(userId), anyList()))
                    .thenAnswer(inv -> inv.<List<Long>>getArgument(1).stream().distinct().count());
        }

        @Test
        @DisplayName("유사한 메모가 있으면 results를 반환하고 message는 null이다")
        void recommendMemos_hasResults_returnsResultsWithNullMessage() {
            // given
            Memo memo = Memo.builder().id(5L).title("추천 메모").content("내용").user(user).isDeleted(false).build();
            MemoRecommendationRequest request = new MemoRecommendationRequest(List.of(1L, 2L, 3L));
            double candidateThreshold = 0.7;

            given(memoRecommendationProperties.getCandidateSimilarityThreshold()).willReturn(candidateThreshold);
            given(vectorStoreRepository.computeSelectionCohesion(eq(userId), eq(List.of(1L, 2L, 3L))))
                    .willReturn(null);
            given(vectorStoreRepository.findRecommendedMemoIds(eq(userId), eq(List.of(1L, 2L, 3L)), eq(candidateThreshold)))
                    .willReturn(List.of(5L));
            given(memoRepository.findByIdInWithTagsAndNotDeleted(eq(userId), eq(List.of(5L))))
                    .willReturn(List.of(memo));

            // when
            MemoRecommendationResponse response = memoService.recommendMemos(userId, request);

            // then
            assertThat(response.results()).hasSize(1);
            assertThat(response.results().get(0).memoId()).isEqualTo(5L);
            assertThat(response.results().get(0).title()).isEqualTo("추천 메모");
            assertThat(response.message()).isNull();
            verify(memoRecommendationProperties).getCandidateSimilarityThreshold();
            verify(memoRecommendationProperties, never()).getSingleSelectionSimilarityThreshold();
        }

        @Test
        @DisplayName("유사도 임계값 미만이면 빈 results와 안내 메시지를 반환한다")
        void recommendMemos_noResults_returnsMessageWithEmptyResults() {
            // given
            MemoRecommendationRequest request = new MemoRecommendationRequest(List.of(1L, 2L, 3L));
            double candidateThreshold = 0.7;

            given(memoRecommendationProperties.getCandidateSimilarityThreshold()).willReturn(candidateThreshold);
            given(vectorStoreRepository.computeSelectionCohesion(eq(userId), eq(List.of(1L, 2L, 3L))))
                    .willReturn(null);
            given(vectorStoreRepository.findRecommendedMemoIds(eq(userId), eq(List.of(1L, 2L, 3L)), eq(candidateThreshold)))
                    .willReturn(List.of());

            // when
            MemoRecommendationResponse response = memoService.recommendMemos(userId, request);

            // then
            assertThat(response.results()).isEmpty();
            assertThat(response.message()).isEqualTo("선택한 메모와 관련된 메모를 찾지 못했어요.");
        }

        @Test
        @DisplayName("추천 결과가 3개를 초과해도 최대 3개만 반환한다")
        void recommendMemos_moreThanThree_returnsOnlyTop3() {
            // given
            Memo memo1 = Memo.builder().id(10L).title("추천1").content("내용").user(user).isDeleted(false).build();
            Memo memo2 = Memo.builder().id(11L).title("추천2").content("내용").user(user).isDeleted(false).build();
            Memo memo3 = Memo.builder().id(12L).title("추천3").content("내용").user(user).isDeleted(false).build();
            MemoRecommendationRequest request = new MemoRecommendationRequest(List.of(1L));
            double singleThreshold = 0.5;

            given(memoRecommendationProperties.getSingleSelectionSimilarityThreshold()).willReturn(singleThreshold);
            given(vectorStoreRepository.findRecommendedMemoIds(eq(userId), eq(List.of(1L)), eq(singleThreshold)))
                    .willReturn(List.of(10L, 11L, 12L, 13L, 14L)); // 5개 반환
            // 단일 선택은 실제 리포지토리에서도 항상 null(응집도 정의 불가)이라 스텁 불필요하지만
            // 기본 목 동작(0.0)과 무관하게 Gate1을 확실히 건너뛰도록 명시한다.
            given(vectorStoreRepository.computeSelectionCohesion(eq(userId), eq(List.of(1L))))
                    .willReturn(null);
            given(memoRepository.findByIdInWithTagsAndNotDeleted(eq(userId), eq(List.of(10L, 11L, 12L))))
                    .willReturn(List.of(memo1, memo2, memo3));

            // when
            MemoRecommendationResponse response = memoService.recommendMemos(userId, request);

            // then
            assertThat(response.results()).hasSize(3);
            verify(memoRepository).findByIdInWithTagsAndNotDeleted(eq(userId), eq(List.of(10L, 11L, 12L)));
            verify(memoRecommendationProperties).getSingleSelectionSimilarityThreshold();
            verify(memoRecommendationProperties, never()).getCandidateSimilarityThreshold();
        }

        @Test
        @DisplayName("선택한 메모들의 응집도가 임계값 미만이면 후보 검색 없이 빈 results와 안내 메시지를 반환한다")
        void recommendMemos_lowCohesion_returnsMessageWithEmptyResultsWithoutCandidateSearch() {
            // given
            MemoRecommendationRequest request = new MemoRecommendationRequest(List.of(1L, 2L, 3L));
            double cohesionThreshold = 0.3;

            given(memoRecommendationProperties.getCohesionThreshold()).willReturn(cohesionThreshold);
            given(vectorStoreRepository.computeSelectionCohesion(eq(userId), eq(List.of(1L, 2L, 3L))))
                    .willReturn(0.1);

            // when
            MemoRecommendationResponse response = memoService.recommendMemos(userId, request);

            // then
            assertThat(response.results()).isEmpty();
            assertThat(response.message()).isEqualTo("선택한 메모들끼리 연관성이 없어요.");
            verify(vectorStoreRepository, never()).findRecommendedMemoIds(any(), any(), anyDouble());
        }

        @Test
        @DisplayName("선택한 메모가 본인 것이 아니거나 없으면 SOURCE_MEMO_NOT_FOUND 예외가 발생한다")
        void recommendMemos_notOwnedOrMissing_throws() {
            // given: 2개를 선택했지만 본인 소유(미삭제)는 1개뿐
            MemoRecommendationRequest request = new MemoRecommendationRequest(List.of(1L, 2L));
            given(memoRepository.countByIdInAndUserIdAndNotDeleted(eq(userId), eq(List.of(1L, 2L))))
                    .willReturn(1L);

            // when & then
            assertThatThrownBy(() -> memoService.recommendMemos(userId, request))
                    .isInstanceOf(MemoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemoErrorCode.SOURCE_MEMO_NOT_FOUND);

            verify(vectorStoreRepository, never()).computeSelectionCohesion(any(), any());
        }
    }

    @Nested
    @DisplayName("메모 수정 테스트")
    class UpdateMemo {

        private User user;

        @BeforeEach
        void setUp() {
            user = User.builder().id(userId).build();
            ReflectionTestUtils.setField(memoService, "ioExecutor", (Executor) Runnable::run);
            lenient().when(s3Util.getObjectMetadata(anyString()))
                    .thenReturn(new S3Util.S3ObjectMetadata(1_024L, "application/octet-stream"));
        }

        private Memo ownedMemo(String title, String content) {
            Memo memo = Memo.createMemo(title, content, user);
            ReflectionTestUtils.setField(memo, "id", memoId);
            return memo;
        }

        private MemoUpdateRequest request(String title, String content) {
            return new MemoUpdateRequest(title, content, List.of(), List.of(), List.of());
        }

        @Test
        @DisplayName("제목/본문을 수정하고 응답에 updatedAt을 담아 반환한다")
        void updateMemo_updatesTitleAndContent() {
            // given
            Memo memo = ownedMemo("옛 제목", "옛 내용");
            LocalDateTime updatedAt = LocalDateTime.of(2026, 8, 24, 12, 0);
            ReflectionTestUtils.setField(memo, "updatedAt", updatedAt);
            given(memoRepository.findByIdAndNotDeleted(memoId)).willReturn(Optional.of(memo));

            // when
            MemoResponse response = memoService.updateMemo(userId, memoId, request("새 제목", "새 내용"));

            // then
            assertThat(memo.getTitle()).isEqualTo("새 제목");
            assertThat(memo.getContent()).isEqualTo("새 내용");
            assertThat(response.memoId()).isEqualTo(memoId);
            assertThat(response.title()).isEqualTo("새 제목");
            assertThat(response.updatedAt()).isEqualTo(updatedAt);
        }

        @Test
        @DisplayName("제목/본문이 바뀌면 텍스트 재임베딩 이벤트를 발행한다")
        void updateMemo_contentChanged_publishesTextUpdatedEvent() {
            // given
            Memo memo = ownedMemo("옛 제목", "옛 내용");
            given(memoRepository.findByIdAndNotDeleted(memoId)).willReturn(Optional.of(memo));

            // when
            memoService.updateMemo(userId, memoId, request("옛 제목", "완전 다른 내용"));

            // then
            verify(eventPublisher).publishEvent(any(MemoTextUpdatedEvent.class));
        }

        @Test
        @DisplayName("제목/본문이 그대로면 텍스트 재임베딩 이벤트를 발행하지 않는다 (임베딩 쿼터 절약)")
        void updateMemo_contentUnchanged_doesNotPublishTextUpdatedEvent() {
            // given
            Memo memo = ownedMemo("제목", "내용");
            given(memoRepository.findByIdAndNotDeleted(memoId)).willReturn(Optional.of(memo));

            // when: 제목/본문 동일, 태그만 다르게
            memoService.updateMemo(userId, memoId,
                    new MemoUpdateRequest("제목", "내용", List.of("새태그"), List.of(), List.of()));

            // then
            verify(eventPublisher, never()).publishEvent(any(MemoTextUpdatedEvent.class));
        }

        @Test
        @DisplayName("존재하지 않는 메모를 수정하면 MEMO_NOT_FOUND 예외가 발생한다")
        void updateMemo_notFound_throws() {
            given(memoRepository.findByIdAndNotDeleted(memoId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> memoService.updateMemo(userId, memoId, request("t", "c")))
                    .isInstanceOf(MemoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemoErrorCode.MEMO_NOT_FOUND);
        }

        @Test
        @DisplayName("본인 메모가 아니면 FORBIDDEN_MEMO 예외가 발생하고 아무것도 바꾸지 않는다")
        void updateMemo_notOwner_throws() {
            // given: 메모 주인은 userId, 침입자는 999
            Memo memo = ownedMemo("제목", "내용");
            given(memoRepository.findByIdAndNotDeleted(memoId)).willReturn(Optional.of(memo));

            // when & then
            assertThatThrownBy(() -> memoService.updateMemo(999L, memoId, request("해킹", "해킹")))
                    .isInstanceOf(MemoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemoErrorCode.FORBIDDEN_MEMO);

            assertThat(memo.getTitle()).isEqualTo("제목");
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("tagNames가 현재 태그와 동일하면 태그를 재생성하지 않는다 (자동저장 불필요 churn 방지)")
        void updateMemo_tagNamesUnchanged_skipsReplacement() {
            // given: 현재 태그 ["SOPT"], 요청도 동일
            Memo memo = ownedMemo("제목", "내용");
            memo.addTag(Tag.create("SOPT", user), 0);
            given(memoRepository.findByIdAndNotDeleted(memoId)).willReturn(Optional.of(memo));

            // when
            memoService.updateMemo(userId, memoId,
                    new MemoUpdateRequest("제목", "내용", List.of("SOPT"), List.of(), List.of()));

            // then: 태그 그대로 유지 + 태그 조회/생성 로직 미호출
            assertThat(memo.getTags()).extracting(Tag::getName).containsExactly("SOPT");
            verify(tagRepository, never()).findAllByNameInAndUser(anyList(), any());
        }

        @Test
        @DisplayName("tagNames가 빈 배열이면 태그를 전부 제거한다")
        void updateMemo_tagNamesEmpty_removesAll() {
            // given
            Memo memo = ownedMemo("제목", "내용");
            memo.addTag(Tag.create("기존태그", user), 0);
            given(memoRepository.findByIdAndNotDeleted(memoId)).willReturn(Optional.of(memo));

            // when
            memoService.updateMemo(userId, memoId,
                    new MemoUpdateRequest("제목", "내용", List.of(), List.of(), List.of()));

            // then
            assertThat(memo.getTags()).isEmpty();
        }

        @Test
        @DisplayName("태그를 전체 교체한다 (기존 태그 제거 후 새 태그 부착)")
        void updateMemo_replacesTags() {
            // given: 기존 태그 하나 달린 메모
            Memo memo = ownedMemo("제목", "내용");
            Tag oldTag = Tag.create("옛태그", user);
            memo.addTag(oldTag, 0);
            given(memoRepository.findByIdAndNotDeleted(memoId)).willReturn(Optional.of(memo));
            given(tagRepository.findAllByNameInAndUser(anyList(), any())).willReturn(List.of());
            given(tagRepository.saveAll(anyList()))
                    .willAnswer(inv -> inv.getArgument(0));

            // when
            memoService.updateMemo(userId, memoId,
                    new MemoUpdateRequest("제목", "내용", List.of("새태그"), List.of(), List.of()));

            // then
            assertThat(memo.getTags()).extracting(Tag::getName).containsExactly("새태그");
        }

        @Test
        @DisplayName("새 이미지를 추가하면 저장하고 이미지 임베딩 이벤트를 발행한다")
        void updateMemo_addsNewImage() {
            // given
            Memo memo = ownedMemo("제목", "내용");
            given(memoRepository.findByIdAndNotDeleted(memoId)).willReturn(Optional.of(memo));
            given(memoImageRepository.saveAll(anyList()))
                    .willAnswer(inv -> {
                        List<MemoImage> imgs = inv.getArgument(0);
                        long id = 500L;
                        for (MemoImage img : imgs) {
                            ReflectionTestUtils.setField(img, "id", id++);
                        }
                        return imgs;
                    });

            MemoUpdateRequest.ImageEdit newImage =
                    new MemoUpdateRequest.ImageEdit(null, "memo-image/1/new.png", "new.png", 0);
            MemoUpdateRequest req =
                    new MemoUpdateRequest("제목", "내용", List.of(), List.of(newImage), List.of());

            // when
            memoService.updateMemo(userId, memoId, req);

            // then
            verify(s3KeyUtil).validateS3Key(userId, "memo-image", "memo-image/1/new.png");
            verify(memoImageRepository).saveAll(anyList());
            verify(eventPublisher).publishEvent(any(MemoImageCreatedEvent.class));
        }

        @Test
        @DisplayName("빠진 이미지는 삭제하고 첨부 제거 이벤트(벡터/S3 정리)를 발행한다")
        void updateMemo_removesImage() {
            // given: 이미지 2개 달린 메모, 요청엔 하나만 유지
            Memo memo = ownedMemo("제목", "내용");
            MemoImage keep = MemoImage.builder()
                    .id(10L).memo(memo).imageS3Key("memo-image/1/keep.png").imagePriority(0).build();
            MemoImage remove = MemoImage.builder()
                    .id(11L).memo(memo).imageS3Key("memo-image/1/remove.png").imagePriority(1).build();
            memo.getMemoImages().add(keep);
            memo.getMemoImages().add(remove);
            given(memoRepository.findByIdAndNotDeleted(memoId)).willReturn(Optional.of(memo));

            MemoUpdateRequest.ImageEdit keepEdit =
                    new MemoUpdateRequest.ImageEdit(10L, null, null, 0);
            MemoUpdateRequest req =
                    new MemoUpdateRequest("제목", "내용", List.of(), List.of(keepEdit), List.of());

            // when
            memoService.updateMemo(userId, memoId, req);

            // then
            verify(memoImageRepository).deleteAllByIdInBatch(List.of(11L));

            ArgumentCaptor<MemoAttachmentsRemovedEvent> captor =
                    ArgumentCaptor.forClass(MemoAttachmentsRemovedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            MemoAttachmentsRemovedEvent event = captor.getValue();
            assertThat(event.removedImageIds()).containsExactly(11L);
            assertThat(event.removedImageKeys()).containsExactly("memo-image/1/remove.png");
        }

        @Test
        @DisplayName("유지하는 이미지의 정렬 우선순위를 갱신한다")
        void updateMemo_keepImage_updatesPriority() {
            // given
            Memo memo = ownedMemo("제목", "내용");
            MemoImage keep = MemoImage.builder()
                    .id(10L).memo(memo).imageS3Key("memo-image/1/keep.png").imagePriority(0).build();
            memo.getMemoImages().add(keep);
            given(memoRepository.findByIdAndNotDeleted(memoId)).willReturn(Optional.of(memo));

            MemoUpdateRequest.ImageEdit keepEdit =
                    new MemoUpdateRequest.ImageEdit(10L, null, null, 3);
            MemoUpdateRequest req =
                    new MemoUpdateRequest("제목", "내용", List.of(), List.of(keepEdit), List.of());

            // when
            memoService.updateMemo(userId, memoId, req);

            // then
            assertThat(keep.getImagePriority()).isEqualTo(3);
            verify(memoImageRepository, never()).deleteAllByIdInBatch(anyList());
            verify(eventPublisher, never()).publishEvent(any(MemoAttachmentsRemovedEvent.class));
        }

        @Test
        @DisplayName("유지+추가 이미지 합이 5개를 초과하면 TOO_MANY_IMAGES 예외가 발생한다")
        void updateMemo_tooManyImages_throws() {
            // given: 기존 3개 유지 + 신규 3개 = 6개
            Memo memo = ownedMemo("제목", "내용");
            List<MemoUpdateRequest.ImageEdit> edits = new java.util.ArrayList<>();
            for (long i = 1; i <= 3; i++) {
                MemoImage img = MemoImage.builder()
                        .id(i).memo(memo).imageS3Key("memo-image/1/" + i + ".png").imagePriority((int) i).build();
                memo.getMemoImages().add(img);
                edits.add(new MemoUpdateRequest.ImageEdit(i, null, null, (int) i));
            }
            for (int i = 0; i < 3; i++) {
                edits.add(new MemoUpdateRequest.ImageEdit(null, "memo-image/1/new" + i + ".png", "n.png", i));
            }
            MemoUpdateRequest req = new MemoUpdateRequest("제목", "내용", List.of(), edits, List.of());

            // when & then
            assertThatThrownBy(() -> memoService.updateMemo(userId, memoId, req))
                    .isInstanceOf(MemoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemoErrorCode.TOO_MANY_IMAGES);
        }

        @Test
        @DisplayName("이미지 항목이 imageId도 s3Key도 없으면 INVALID_ATTACHMENT_EDIT 예외가 발생한다")
        void updateMemo_imageEditBothNull_throws() {
            // given: 배열 안 원소가 유지(id)도 추가(s3Key)도 아님
            Memo memo = ownedMemo("제목", "내용");
            MemoUpdateRequest.ImageEdit bad =
                    new MemoUpdateRequest.ImageEdit(null, null, null, 0);
            MemoUpdateRequest req =
                    new MemoUpdateRequest("제목", "내용", List.of(), List.of(bad), List.of());

            // when & then
            assertThatThrownBy(() -> memoService.updateMemo(userId, memoId, req))
                    .isInstanceOf(MemoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemoErrorCode.INVALID_ATTACHMENT_EDIT);
        }

        @Test
        @DisplayName("이미지 항목이 imageId와 s3Key를 동시에 가지면 INVALID_ATTACHMENT_EDIT 예외가 발생한다")
        void updateMemo_imageEditBothPresent_throws() {
            // given: 한 항목이 유지(id)와 추가(s3Key)를 동시에 가짐 → 모호
            Memo memo = ownedMemo("제목", "내용");
            MemoImage existing = MemoImage.builder()
                    .id(10L).memo(memo).imageS3Key("memo-image/1/keep.png").imagePriority(0).build();
            memo.getMemoImages().add(existing);
            MemoUpdateRequest.ImageEdit ambiguous =
                    new MemoUpdateRequest.ImageEdit(10L, "memo-image/1/new.png", "new.png", 0);
            MemoUpdateRequest req =
                    new MemoUpdateRequest("제목", "내용", List.of(), List.of(ambiguous), List.of());

            // when & then
            assertThatThrownBy(() -> memoService.updateMemo(userId, memoId, req))
                    .isInstanceOf(MemoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemoErrorCode.INVALID_ATTACHMENT_EDIT);
        }

        @Test
        @DisplayName("파일 항목이 fileId도 s3Key도 없으면 INVALID_ATTACHMENT_EDIT 예외가 발생한다")
        void updateMemo_fileEditBothNull_throws() {
            // given
            Memo memo = ownedMemo("제목", "내용");
            MemoUpdateRequest.FileEdit bad =
                    new MemoUpdateRequest.FileEdit(null, null, null, 0);
            MemoUpdateRequest req =
                    new MemoUpdateRequest("제목", "내용", List.of(), List.of(), List.of(bad));

            // when & then
            assertThatThrownBy(() -> memoService.updateMemo(userId, memoId, req))
                    .isInstanceOf(MemoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemoErrorCode.INVALID_ATTACHMENT_EDIT);
        }

        @Test
        @DisplayName("이 메모의 것이 아닌 imageId를 유지 대상으로 보내면 MEMO_IMAGE_NOT_FOUND 예외가 발생한다")
        void updateMemo_keepUnknownImageId_throws() {
            // given: 메모엔 이미지가 없는데 존재하지 않는 imageId 유지 요청
            Memo memo = ownedMemo("제목", "내용");
            given(memoRepository.findByIdAndNotDeleted(memoId)).willReturn(Optional.of(memo));

            MemoUpdateRequest.ImageEdit ghost =
                    new MemoUpdateRequest.ImageEdit(999L, null, null, 0);
            MemoUpdateRequest req =
                    new MemoUpdateRequest("제목", "내용", List.of(), List.of(ghost), List.of());

            // when & then
            assertThatThrownBy(() -> memoService.updateMemo(userId, memoId, req))
                    .isInstanceOf(MemoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemoErrorCode.MEMO_IMAGE_NOT_FOUND);
        }

        @Test
        @DisplayName("새 첨부는 S3 실제 객체 크기를 조회해 저장한다")
        void updateMemo_addsAttachment_savesActualS3ObjectSize() {
            // given: 요청은 1KB라고 주장하지만 S3 실제 크기는 다름
            Memo memo = ownedMemo("제목", "내용");
            given(memoRepository.findByIdAndNotDeleted(memoId)).willReturn(Optional.of(memo));
            given(s3Util.getObjectMetadata("memo-image/1/new.png"))
                    .willReturn(new S3Util.S3ObjectMetadata(3_000L, "image/png"));
            given(s3Util.getObjectMetadata("memo-file/1/new.pdf"))
                    .willReturn(new S3Util.S3ObjectMetadata(4_000L, "application/pdf"));

            MemoUpdateRequest.ImageEdit newImage =
                    new MemoUpdateRequest.ImageEdit(null, "memo-image/1/new.png", "new.png", 0);
            MemoUpdateRequest.FileEdit newFile =
                    new MemoUpdateRequest.FileEdit(null, "memo-file/1/new.pdf", "new.pdf", 0);
            MemoUpdateRequest req =
                    new MemoUpdateRequest("제목", "내용", List.of(), List.of(newImage), List.of(newFile));

            // when
            memoService.updateMemo(userId, memoId, req);

            // then
            ArgumentCaptor<List<MemoImage>> imageCaptor = ArgumentCaptor.forClass(List.class);
            ArgumentCaptor<List<MemoFile>> fileCaptor = ArgumentCaptor.forClass(List.class);
            verify(memoImageRepository).saveAll(imageCaptor.capture());
            verify(memoFileRepository).saveAll(fileCaptor.capture());

            assertThat(imageCaptor.getValue().get(0).getImageBytes()).isEqualTo(3_000L);
            assertThat(fileCaptor.getValue().get(0).getFileBytes()).isEqualTo(4_000L);
        }

        @Test
        @DisplayName("새 이미지의 S3 실제 크기가 5MB를 초과하면 IMAGE_TOO_LARGE 예외가 발생한다")
        void updateMemo_addedImageActuallyTooLarge_throws() {
            // given: 실제 객체가 5MB 초과
            Memo memo = ownedMemo("제목", "내용");
            given(s3Util.getObjectMetadata("memo-image/1/huge.png"))
                    .willReturn(new S3Util.S3ObjectMetadata(5L * 1024 * 1024 + 1, "image/png"));

            MemoUpdateRequest.ImageEdit huge =
                    new MemoUpdateRequest.ImageEdit(null, "memo-image/1/huge.png", "huge.png", 0);
            MemoUpdateRequest req =
                    new MemoUpdateRequest("제목", "내용", List.of(), List.of(huge), List.of());

            // when & then
            assertThatThrownBy(() -> memoService.updateMemo(userId, memoId, req))
                    .isInstanceOf(MemoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemoErrorCode.IMAGE_TOO_LARGE);

            verify(memoImageRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("새 파일의 S3 실제 크기가 10MB를 초과하면 FILE_TOO_LARGE 예외가 발생한다")
        void updateMemo_addedFileActuallyTooLarge_throws() {
            // given
            Memo memo = ownedMemo("제목", "내용");
            given(s3Util.getObjectMetadata("memo-file/1/huge.pdf"))
                    .willReturn(new S3Util.S3ObjectMetadata(10L * 1024 * 1024 + 1, "application/pdf"));

            MemoUpdateRequest.FileEdit huge =
                    new MemoUpdateRequest.FileEdit(null, "memo-file/1/huge.pdf", "huge.pdf", 0);
            MemoUpdateRequest req =
                    new MemoUpdateRequest("제목", "내용", List.of(), List.of(), List.of(huge));

            // when & then
            assertThatThrownBy(() -> memoService.updateMemo(userId, memoId, req))
                    .isInstanceOf(MemoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemoErrorCode.FILE_TOO_LARGE);

            verify(memoFileRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("새 첨부의 s3Key를 용도별 prefix까지 검증한다")
        void updateMemo_validatesS3KeyPrefixByMediaType() {
            // given
            Memo memo = ownedMemo("제목", "내용");
            given(memoRepository.findByIdAndNotDeleted(memoId)).willReturn(Optional.of(memo));

            MemoUpdateRequest.ImageEdit newImage =
                    new MemoUpdateRequest.ImageEdit(null, "memo-image/1/new.png", "new.png", 0);
            MemoUpdateRequest.FileEdit newFile =
                    new MemoUpdateRequest.FileEdit(null, "memo-file/1/new.pdf", "new.pdf", 0);
            MemoUpdateRequest req =
                    new MemoUpdateRequest("제목", "내용", List.of(), List.of(newImage), List.of(newFile));

            // when
            memoService.updateMemo(userId, memoId, req);

            // then
            verify(s3KeyUtil).validateS3Key(userId, "memo-image", "memo-image/1/new.png");
            verify(s3KeyUtil).validateS3Key(userId, "memo-file", "memo-file/1/new.pdf");
        }

        @Test
        @DisplayName("prefix 검증에 실패하면 첨부를 저장하지 않는다")
        void updateMemo_invalidS3KeyPrefix_doesNotSave() {
            // given: 이미지 자리에 파일 key를 넣은 요청
            Memo memo = ownedMemo("제목", "내용");
            doThrow(new MemoException(MemoErrorCode.INVALID_S3_KEY_FORMAT))
                    .when(s3KeyUtil)
                    .validateS3Key(eq(userId), eq("memo-image"), anyString());

            MemoUpdateRequest.ImageEdit crossed =
                    new MemoUpdateRequest.ImageEdit(null, "memo-file/1/new.pdf", "new.pdf", 0);
            MemoUpdateRequest req =
                    new MemoUpdateRequest("제목", "내용", List.of(), List.of(crossed), List.of());

            // when & then
            assertThatThrownBy(() -> memoService.updateMemo(userId, memoId, req))
                    .isInstanceOf(MemoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemoErrorCode.INVALID_S3_KEY_FORMAT);

            verify(memoImageRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("새 이미지에 imageName이 없으면 MISSING_ATTACHMENT_METADATA 예외가 발생한다")
        void updateMemo_addImageWithoutName_throws() {
            // given
            Memo memo = ownedMemo("제목", "내용");
            MemoUpdateRequest.ImageEdit noName =
                    new MemoUpdateRequest.ImageEdit(null, "memo-image/1/new.png", null, 0);
            MemoUpdateRequest req =
                    new MemoUpdateRequest("제목", "내용", List.of(), List.of(noName), List.of());

            // when & then
            assertThatThrownBy(() -> memoService.updateMemo(userId, memoId, req))
                    .isInstanceOf(MemoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemoErrorCode.MISSING_ATTACHMENT_METADATA);

            verify(memoImageRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("새 첨부의 확장자는 요청이 아니라 s3Key에서 뽑아 저장한다")
        void updateMemo_addedAttachment_extensionParsedFromS3Key() {
            // given
            Memo memo = ownedMemo("제목", "내용");
            given(memoRepository.findByIdAndNotDeleted(memoId)).willReturn(Optional.of(memo));
            given(s3Util.getObjectMetadata("memo-image/1/new.png"))
                    .willReturn(new S3Util.S3ObjectMetadata(3_000L, "image/png"));
            given(s3KeyUtil.extractExtension("memo-image/1/new.png")).willReturn("png");

            MemoUpdateRequest.ImageEdit newImage =
                    new MemoUpdateRequest.ImageEdit(null, "memo-image/1/new.png", "new.png", 0);
            MemoUpdateRequest req =
                    new MemoUpdateRequest("제목", "내용", List.of(), List.of(newImage), List.of());

            // when
            memoService.updateMemo(userId, memoId, req);

            // then
            ArgumentCaptor<List<MemoImage>> captor = ArgumentCaptor.forClass(List.class);
            verify(memoImageRepository).saveAll(captor.capture());
            assertThat(captor.getValue().get(0).getImageExtension()).isEqualTo("png");
        }

        @Test
        @DisplayName("새 파일에 fileName이 없으면 MISSING_ATTACHMENT_METADATA 예외가 발생한다")
        void updateMemo_addFileWithoutName_throws() {
            // given
            Memo memo = ownedMemo("제목", "내용");
            MemoUpdateRequest.FileEdit noName =
                    new MemoUpdateRequest.FileEdit(null, "memo-file/1/new.pdf", null, 0);
            MemoUpdateRequest req =
                    new MemoUpdateRequest("제목", "내용", List.of(), List.of(), List.of(noName));

            // when & then
            assertThatThrownBy(() -> memoService.updateMemo(userId, memoId, req))
                    .isInstanceOf(MemoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemoErrorCode.MISSING_ATTACHMENT_METADATA);

            verify(memoFileRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("유지 항목은 name·extension이 없어도 정상 처리한다")
        void updateMemo_keepWithoutMetadata_succeeds() {
            // given: 자동저장은 유지 항목에 id와 priority만 실어 보낸다
            Memo memo = ownedMemo("제목", "내용");
            MemoImage keepImage = MemoImage.builder()
                    .id(10L).memo(memo).imageS3Key("memo-image/1/keep.png").imagePriority(0).build();
            MemoFile keepFile = MemoFile.builder()
                    .id(20L).memo(memo).fileS3Key("memo-file/1/keep.pdf").filePriority(0).build();
            memo.getMemoImages().add(keepImage);
            memo.getMemoFiles().add(keepFile);
            given(memoRepository.findByIdAndNotDeleted(memoId)).willReturn(Optional.of(memo));

            MemoUpdateRequest req = new MemoUpdateRequest(
                    "제목", "내용", List.of(),
                    List.of(new MemoUpdateRequest.ImageEdit(10L, null, null, 1)),
                    List.of(new MemoUpdateRequest.FileEdit(20L, null, null, 2))
            );

            // when
            memoService.updateMemo(userId, memoId, req);

            // then
            assertThat(keepImage.getImagePriority()).isEqualTo(1);
            assertThat(keepFile.getFilePriority()).isEqualTo(2);
            verify(memoImageRepository, never()).deleteAllByIdInBatch(anyList());
            verify(memoFileRepository, never()).deleteAllByIdInBatch(anyList());
        }

        @Test
        @DisplayName("유지 항목만 있으면 S3 메타데이터를 조회하지 않는다")
        void updateMemo_keepOnly_doesNotQueryS3Metadata() {
            // given: 자동저장처럼 첨부 변경 없이 본문만 수정
            Memo memo = ownedMemo("제목", "내용");
            MemoImage keep = MemoImage.builder()
                    .id(10L).memo(memo).imageS3Key("memo-image/1/keep.png").imagePriority(0).build();
            memo.getMemoImages().add(keep);
            given(memoRepository.findByIdAndNotDeleted(memoId)).willReturn(Optional.of(memo));

            MemoUpdateRequest.ImageEdit keepEdit =
                    new MemoUpdateRequest.ImageEdit(10L, null, null, 0);
            MemoUpdateRequest req =
                    new MemoUpdateRequest("제목", "새 내용", List.of(), List.of(keepEdit), List.of());

            // when
            memoService.updateMemo(userId, memoId, req);

            // then
            verify(s3Util, never()).getObjectMetadata(anyString());
            verify(s3KeyUtil, never()).validateS3Key(anyLong(), anyString(), anyString());
        }
    }
}
