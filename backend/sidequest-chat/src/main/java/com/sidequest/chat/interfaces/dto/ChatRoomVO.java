package com.sidequest.chat.interfaces.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatRoomVO {
    private Long id;
    private String name; // Room name or other user's nickname
    private String type;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Integer unreadCount;
    private String recipientNickname;
    private String recipientAvatar;
}

