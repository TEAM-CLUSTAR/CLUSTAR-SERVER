package org.project.domain.memo.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.domain.label.repository.LabelRepository;
import org.project.domain.memo.dto.request.MemoPresignedUrlRequest;
import org.project.domain.memo.dto.response.MemoDetailResponse;
import org.project.domain.memo.dto.response.MemoPresignedUrlResponse;
import org.project.domain.memo.entity.Memo;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemoServiceImplTest {

    @Mock private MemoRepository memoRepository;
    @Mock private UserRepository userRepository;
    @Mock private LabelRepository labelRepository;

    @Mock private MemoImageRepository memoImageRepository;
    @Mock private MemoFileRepository memoFileRepository;
    @Mock private MemoLabelRepository memoLabelRepository;

    @Mock private S3KeyUtil s3KeyUtil;
    @Mock private S3Util s3Util;

    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MemoServiceImpl memoService;

    private final Long userId = 1L;
    private final Long memoId = 100L;

    @Nested
    class getOneMemoDetail{
        @DisplayName("메모 모달창을 상세 조회할 수 있어야 한다.")
        @Test
        void getOneMemoDetail_Success() {
            // given 준비
            User user = User.builder().id(userId).build();
            Memo memo = Memo.createMemo("테스트 제목", "테스트 내용", user);
            given(memoRepository.findByIdAndNotDeleted(memoId)).willReturn(Optional.of(memo));
            // 이미지 넣을 때 실행
            // given(s3Util.generatePresignedUrl(any())).willReturn("https://s3.example.com/test-image.jpg");

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
    class deleteMemo{

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
    class issuePresignedUrls{

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