package org.project.domain.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
            summary = "최근 AI 채팅방 단일 조회",
            description = "로그인한 사용자의 최근 AI 채팅방을 조회합니다."
    )
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

