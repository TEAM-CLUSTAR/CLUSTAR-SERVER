package org.project.domain.ai.service;

import org.project.domain.ai.dto.request.MemoAiRequest;
import org.project.domain.ai.dto.request.MemoAiRequestForPlan;
import org.project.domain.ai.dto.response.MemoAiResponse;
import org.project.domain.ai.dto.response.MemoAiResponseForPlan;

public interface MemoAiService {

    MemoAiResponse generate(
            Long userId,
            Long chatRoomId,
            MemoAiRequest request
    );

    MemoAiResponseForPlan generateForPlan(
            Long userId,
            Long chatRoomId,
            MemoAiRequestForPlan request
    );
}
