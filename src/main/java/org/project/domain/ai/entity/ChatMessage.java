package org.project.domain.ai.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.domain.ai.dto.MemoAiOptions;
import org.project.domain.ai.entity.converter.MemoIdsConverter;
import org.project.global.entity.BaseEntity;

import java.util.List;

/**
 * AI 채팅방의 대화 한 건.
 * <p>
 * spring_ai_chat_memory는 LLM에 주입할 최근 컨텍스트만 유지하므로(윈도우 초과분 물리 삭제),
 * 화면 복원용 대화 기록은 이 엔티티에 별도로 보관한다.
 * <p>
 * 채팅방이 soft delete되면 조회 경로가 함께 사라지므로 별도의 isDeleted 플래그는 두지 않는다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_message")
public class ChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_message_id")
    private Long id;

    // ChatRoom(1) : ChatMessage(N)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private ChatMessageRole role;

    // AI 호출이 실패한 자리(status = FAILED)에는 내용이 없다.
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ChatMessageStatus status;

    // AI 응답에만 존재
    @Column(name = "title")
    private String title;

    // 요청에 적용된 옵션. USER/ASSISTANT 모두 보관해 재시도 시 그대로 복원할 수 있다.
    @Enumerated(EnumType.STRING)
    @Column(name = "option")
    private MemoAiOptions option;

    // 요청이 참조한 메모 목록. USER 메시지에도 보관해야 실패한 턴을 재시도할 수 있다.
    @Convert(converter = MemoIdsConverter.class)
    @Column(name = "memo_ids", columnDefinition = "TEXT")
    private List<Long> memoIds;

    /**
     * 사용자가 보낸 질문. option/memoIds를 함께 보관하므로
     * 이 한 건만으로 같은 요청을 재구성할 수 있다.
     */
    public static ChatMessage user(
            ChatRoom chatRoom,
            String content,
            MemoAiOptions option,
            List<Long> memoIds
    ) {
        ChatMessage message = new ChatMessage();
        message.chatRoom = chatRoom;
        message.role = ChatMessageRole.USER;
        message.status = ChatMessageStatus.SUCCESS;
        message.content = content;
        message.option = option;
        message.memoIds = (memoIds == null) ? List.of() : memoIds;
        return message;
    }

    public static ChatMessage assistant(
            ChatRoom chatRoom,
            String title,
            String content,
            MemoAiOptions option,
            List<Long> memoIds
    ) {
        ChatMessage message = new ChatMessage();
        message.chatRoom = chatRoom;
        message.role = ChatMessageRole.ASSISTANT;
        message.status = ChatMessageStatus.SUCCESS;
        message.title = title;
        message.content = content;
        message.option = option;
        message.memoIds = (memoIds == null) ? List.of() : memoIds;
        return message;
    }

    /**
     * AI 호출이 실패해 응답을 받지 못한 자리.
     * <p>
     * 화면에 보여줄 문구는 프론트가 정하므로 content를 채우지 않는다.
     * 재시도에 필요한 option/memoIds는 짝이 되는 USER 메시지에 있다.
     */
    public static ChatMessage failed(ChatRoom chatRoom) {
        ChatMessage message = new ChatMessage();
        message.chatRoom = chatRoom;
        message.role = ChatMessageRole.ASSISTANT;
        message.status = ChatMessageStatus.FAILED;
        message.memoIds = List.of();
        return message;
    }
}
