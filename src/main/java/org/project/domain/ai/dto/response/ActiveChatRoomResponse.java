package org.project.domain.ai.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.project.domain.ai.dto.MemoAiOptions;
import org.project.domain.ai.entity.ChatMessage;

import java.time.LocalDateTime;
import java.util.List;

@Schema(requiredProperties = {"chatRoomId", "messages"})
public record ActiveChatRoomResponse(

        @Schema(description = "활성 채팅방 ID")
        Long chatRoomId,

        @Schema(description = "대화 목록. 비어 있으면 아직 전송한 프롬프트가 없다는 의미")
        List<ChatMessageResponse> messages
) {

    public static ActiveChatRoomResponse of(Long chatRoomId, List<ChatMessageResponse> messages) {
        return new ActiveChatRoomResponse(chatRoomId, messages);
    }

    @Schema(requiredProperties = {"messageId", "role", "status", "memoIds", "createdAt"})
    public record ChatMessageResponse(

            Long messageId,

            @Schema(description = "USER | ASSISTANT")
            String role,

            @Schema(description = "SUCCESS | FAILED. FAILED는 AI 호출이 실패한 자리로 content가 없다")
            String status,

            @Schema(description = "AI 응답의 제목. USER 메시지와 실패한 응답은 null")
            String title,

            @Schema(description = "본문. status가 FAILED면 null이며, 표시할 문구는 클라이언트가 정한다")
            String content,

            @Schema(description = "요청에 적용된 옵션. 재시도 시 그대로 사용할 수 있다")
            MemoAiOptions option,

            @Schema(description = "요청이 참조한 메모 ID 목록. 재시도 시 그대로 사용할 수 있다")
            List<Long> memoIds,

            LocalDateTime createdAt
    ) {
        public static ChatMessageResponse from(ChatMessage message) {
            return new ChatMessageResponse(
                    message.getId(),
                    message.getRole().name(),
                    message.getStatus().name(),
                    message.getTitle(),
                    message.getContent(),
                    message.getOption(),
                    message.getMemoIds(),
                    message.getCreatedAt()
            );
        }
    }
}
