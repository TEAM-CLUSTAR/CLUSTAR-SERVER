package org.project.domain.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.project.domain.ai.dto.response.ActiveChatRoomResponse;
import org.project.domain.ai.dto.response.ChatRoomListResponse;
import org.project.domain.ai.dto.response.CreateChatRoomResponse;
import org.project.domain.ai.entity.ChatRoom;
import org.project.domain.ai.service.ChatRoomService;
import org.project.domain.user.dto.CustomUserDetails;
import org.project.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chat-rooms")
@RequiredArgsConstructor
@Tag(
        name = "AI Chat Room",
        description = "AI 채팅방(대화 세션) 관리 API"
)
public class ChatRoomController {

    private final ChatRoomService chatRoomService;


    @Operation(
            summary = "AI 채팅방 생성 (새 대화 시작)",
            description = """
                새로운 AI 채팅방을 생성합니다.

                기존 활성 채팅방이 있으면 함께 정리합니다.
                - 채팅방: soft delete
                - 대화 컨텍스트(ChatMemory): 삭제

                패널을 여는 용도로는 호출하지 마세요. 기존 대화가 삭제됩니다.
                패널 진입 시에는 `GET /api/v1/chat-rooms/active`를 사용합니다.
                """)
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
            summary = "활성 AI 채팅방 및 대화 조회",
            description = """
                AI 패널 진입 시 사용합니다.
                활성 채팅방과 지금까지의 대화를 함께 반환합니다.

                - 활성 채팅방이 없으면 새로 생성한 뒤 빈 대화를 반환합니다 (404 없음)
                - `messages`가 비어 있으면 아직 전송한 프롬프트가 없다는 의미입니다
                - `messages`는 대화 순서(오름차순)로 정렬됩니다
                """
    )
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<ActiveChatRoomResponse>> getActiveConversation(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        chatRoomService.getActiveConversation(userDetails.getUserId())
                )
        );
    }


    @Operation(
            summary = "AI 채팅방 전체 조회",
            description = "로그인한 사용자의 AI 채팅방 목록을 조회합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<ChatRoomListResponse>> getChatRooms(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        chatRoomService.findAllByUser(userDetails.getUserId())
                )
        );
    }

    @Operation(
            summary = "[Deprecated] 최근 AI 채팅방 단일 조회",
            description = """
                로그인한 사용자의 최근 AI 채팅방을 조회합니다.

                대화 내용을 함께 반환하는 `GET /api/v1/chat-rooms/active` 사용을 권장합니다.
                """,
            deprecated = true
    )
    @Deprecated
    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<ChatRoomListResponse.ChatRoomResponse>> findLatestChatRoomByUser(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        ChatRoomListResponse.ChatRoomResponse response = chatRoomService.findLatestChatRoomByUser(userDetails.getUserId());

        return ResponseEntity.ok(ApiResponse.ok(response));
    }


    @Operation(
            summary = "AI 채팅방 삭제",
            description = "AI 채팅방을 삭제합니다. (Soft Delete)"
    )
    @DeleteMapping("/{chatRoomId}")
    public ResponseEntity<ApiResponse<Void>> deleteChatRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long chatRoomId
    ) {
        chatRoomService.delete(userDetails.getUserId(), chatRoomId);

        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}


