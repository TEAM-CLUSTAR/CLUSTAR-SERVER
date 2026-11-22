package org.project.domain.memo.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemoEventListener 테스트")
class MemoEventListenerTest {

    @InjectMocks
    private MemoEventListener listener;

    @Mock
    private S3DeletionHandler s3DeletionHandler;

    @Test
    @DisplayName("영구 삭제 S3 정리 중 한 키가 실패 기록까지 실패해도 나머지 키는 계속 지운다")
    void handleMemoPurged_continuesAfterOneKeyFails() {
        MemoPurgedEvent event = new MemoPurgedEvent(List.of(
                new MemoPurgedEvent.PurgedMemo(1L, List.of("img-1", "img-2"), List.of("file-1")),
                new MemoPurgedEvent.PurgedMemo(2L, List.of("img-3"), List.of())
        ));
        doThrow(new IllegalStateException("db down"))
                .when(s3DeletionHandler).deleteOrRecord("img-1", 1L, "image");

        assertThatCode(() -> listener.handleMemoPurged(event)).doesNotThrowAnyException();

        verify(s3DeletionHandler).deleteOrRecord("img-2", 1L, "image");
        verify(s3DeletionHandler).deleteOrRecord("file-1", 1L, "file");
        verify(s3DeletionHandler).deleteOrRecord("img-3", 2L, "image");
    }
}
