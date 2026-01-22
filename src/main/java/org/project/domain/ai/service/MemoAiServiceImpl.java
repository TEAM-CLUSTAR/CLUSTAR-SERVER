package org.project.domain.ai.service;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.dto.request.MemoAiRequest;
import org.project.domain.ai.dto.response.MemoAiResponse;
import org.project.domain.ai.rag.pipeline.RagPipeline;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemoAiServiceImpl implements MemoAiService {

    private final RagPipeline ragPipeline;
    private final ChatRoomService chatRoomService;

    @Override
    public MemoAiResponse generate(
            Long userId,
            Long chatRoomId,
            MemoAiRequest request
    ) {

        // 접근 검증
        chatRoomService.validateAccess(userId, chatRoomId);

        // 파이프라인 실행
        return ragPipeline.run(
                userId,
                chatRoomId,
                request
        );
    }
}
