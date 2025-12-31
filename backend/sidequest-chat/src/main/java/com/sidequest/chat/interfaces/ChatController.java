package com.sidequest.chat.interfaces;

import com.sidequest.chat.application.ChatService;
import com.sidequest.chat.infrastructure.ChatMessageDO;
import com.sidequest.chat.interfaces.dto.ChatRoomMemberVO;
import com.sidequest.chat.interfaces.dto.ChatRoomVO;
import com.sidequest.chat.interfaces.dto.CreateRoomRequest;
import com.sidequest.common.Result;
import com.sidequest.common.context.UserContext;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @GetMapping("/rooms")
    public Result<List<ChatRoomVO>> getRooms() {
        Long userId = Long.valueOf(UserContext.getUserId());
        return Result.success(chatService.getUserRooms(userId));
    }

    @PostMapping("/rooms")
    public Result<ChatRoomVO> createRoom(@RequestBody CreateRoomRequest request) {
        Long userId = Long.valueOf(UserContext.getUserId());
        return Result.success(chatService.createRoom(userId, request));
    }

    @GetMapping("/rooms/find")
    public Result<ChatRoomVO> findOrCreateRoom(@RequestParam Long recipientId) {
        Long userId = Long.valueOf(UserContext.getUserId());
        return Result.success(chatService.findOrCreatePrivateRoom(userId, recipientId));
    }

    @DeleteMapping("/rooms/{roomId}")
    public Result<String> deleteRoom(@PathVariable Long roomId) {
        Long userId = Long.valueOf(UserContext.getUserId());
        boolean deleted = chatService.deleteRoom(roomId, userId);
        if (!deleted) {
            return Result.forbidden();
        }
        return Result.success("Room deleted");
    }

    @GetMapping("/rooms/{roomId}/members")
    public Result<List<ChatRoomMemberVO>> getRoomMembers(@PathVariable Long roomId) {
        return Result.success(chatService.getRoomMembers(roomId));
    }

    @GetMapping("/rooms/{roomId}/messages")
    public Result<List<ChatMessageDO>> getMessages(
            @PathVariable Long roomId,
            @RequestParam(required = false) Long sinceId) {
        return Result.success(chatService.getMessages(roomId, sinceId));
    }

    @PostMapping("/rooms/{roomId}/send")
    public Result<ChatMessageDO> sendMessage(
            @PathVariable Long roomId,
            @RequestBody SendMessageRequest request) {
        Long userId = Long.valueOf(UserContext.getUserId());
        return Result.success(chatService.sendMessage(userId, roomId, request.getContent()));
    }

    @PostMapping("/rooms/{roomId}/read")
    public Result<String> markAsRead(@PathVariable Long roomId) {
        Long userId = Long.valueOf(UserContext.getUserId());
        chatService.markAsRead(roomId, userId);
        return Result.success("Marked as read");
    }

    @Data
    public static class SendMessageRequest {
        private String content;
    }
}

