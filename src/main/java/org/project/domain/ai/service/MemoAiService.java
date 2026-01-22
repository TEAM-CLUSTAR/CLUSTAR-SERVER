package org.project.domain.ai.service;

import org.project.domain.ai.dto.request.MemoAiRequest;
import org.project.domain.ai.dto.response.MemoAiResponse;

public interface MemoAiService {

    MemoAiResponse generate(
            Long userId,
            Long chatRoomId,
            MemoAiRequest request
    );
}
