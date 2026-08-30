package org.project.domain.ai.service;

import org.project.domain.ai.dto.request.MemoAiRequest;
import org.project.domain.ai.dto.response.ActiveChatRoomResponse;
import org.project.domain.ai.dto.response.MemoAiResponse;

import java.util.List;

public interface ChatMessageService {

    List<ActiveChatRoomResponse.ChatMessageResponse> findByChatRoomId(Long chatRoomId);

    /**
     * 사용자 프롬프트와 AI 응답을 한 번에 저장한다.
     */
    void saveTurn(Long chatRoomId, MemoAiRequest request, MemoAiResponse response);

    /**
     * AI 호출이 실패한 턴을 저장한다.
     * 사용자 질문은 그대로 남기고 AI 응답 자리는 FAILED로 비워둔다.
     */
    void saveFailedTurn(Long chatRoomId, MemoAiRequest request);
}
