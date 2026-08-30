package org.project.domain.ai.service;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.dto.request.MemoAiRequest;
import org.project.domain.ai.dto.response.ActiveChatRoomResponse;
import org.project.domain.ai.dto.response.MemoAiResponse;
import org.project.domain.ai.entity.ChatMessage;
import org.project.domain.ai.entity.ChatRoom;
import org.project.domain.ai.repository.ChatMessageRepository;
import org.project.domain.ai.repository.ChatRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;

    @Override
    public List<ActiveChatRoomResponse.ChatMessageResponse> findByChatRoomId(Long chatRoomId) {
        return chatMessageRepository.findByChatRoomIdOrderByIdAsc(chatRoomId)
                .stream()
                .map(ActiveChatRoomResponse.ChatMessageResponse::from)
                .toList();
    }

    /**
     * RAG 호출은 수 초가 걸리므로 트랜잭션 밖에서 수행하고,
     * 저장만 이 짧은 쓰기 트랜잭션으로 분리한다.
     */
    @Override
    @Transactional
    public void saveTurn(Long chatRoomId, MemoAiRequest request, MemoAiResponse response) {
        ChatRoom chatRoom = chatRoomRepository.getReferenceById(chatRoomId);

        chatMessageRepository.save(saveUser(chatRoom, request));
        chatMessageRepository.save(ChatMessage.assistant(
                chatRoom,
                response.title(),
                response.content(),
                response.option(),
                response.memoIds()
        ));
    }

    /**
     * 실패한 턴도 대화에 남긴다. 사용자가 나중에 스크롤을 올려
     * 무엇을 요청했다가 실패했는지 확인하고 그대로 재시도할 수 있어야 한다.
     */
    @Override
    @Transactional
    public void saveFailedTurn(Long chatRoomId, MemoAiRequest request) {
        ChatRoom chatRoom = chatRoomRepository.getReferenceById(chatRoomId);

        chatMessageRepository.save(saveUser(chatRoom, request));
        chatMessageRepository.save(ChatMessage.failed(chatRoom));
    }

    private ChatMessage saveUser(ChatRoom chatRoom, MemoAiRequest request) {
        return ChatMessage.user(
                chatRoom,
                request.userPrompt(),
                request.option(),
                request.memoIds()
        );
    }
}
