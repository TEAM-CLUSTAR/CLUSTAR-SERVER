package org.project.domain.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.project.domain.ai.dto.response.CreateChatRoomResponse;
import org.project.domain.ai.entity.ChatRoom;
import org.project.domain.ai.service.ChatRoomService;
import org.project.domain.user.dto.CustomUserDetails;
import org.project.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/chat-rooms")
@RequiredArgsConstructor
@Tag(
        name = "AI Chat Room",
        description = "AI 채팅방(대화 세션) 관리 API"
)
public class ChatRoomController {

    private final ChatRoomService chatRoomService;


    @Operation(
            summary = "AI 채팅방 생성",
            description = "새로운 AI 채팅방을 생성합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<CreateChatRoomResponse>> createChatRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ChatRoom chatRoom = chatRoomService.create(userDetails.getUserId());

        return ResponseEntity.ok(
                ApiResponse.ok(
                        CreateChatRoomResponse.of(chatRoom.getId())
                )
        );
    }

    @Operation(
            summary = "AI 채팅방 삭제",
            description = """
                    AI 채팅방을 삭제합니다. (Soft Delete)
                    
                    - 실제 데이터는 삭제되지 않고 isDeleted = true 처리됩니다.
                    - 삭제된 채팅방은 더 이상 AI 요청에 사용할 수 없습니다.
                    - 본인의 채팅방만 삭제할 수 있습니다.
                    """
    )
    @DeleteMapping("/{chatRoomId}")
    public ResponseEntity<ApiResponse<Void>> deleteChatRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long chatRoomId
    ) {
        chatRoomService.delete(userDetails.getUserId(), chatRoomId);

        return ResponseEntity.ok(ApiResponse.ok());
    }
}

