package com.sidequest.chat.interfaces.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatRoomMemberVO {
    private Long userId;
    private String nickname;
    private String avatar;
    private LocalDateTime joinTime;
}
