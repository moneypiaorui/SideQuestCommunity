package com.sidequest.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {
    private String type;         // post_like, post_comment, etc.
    private Long userId;         // 动作发起者
    private Long targetUserId;   // 动作接收者（被通知人）
    private Long targetId;       // 相关对象 ID（如 postId, commentId）
    private String content;      // 简要内容描述
}




