package com.sidequest.identity.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnreadCountVO {
    private Integer chat;          // 私信未读
    private Integer interaction;   // 互动提醒（点赞、评论、收藏）
    private Integer system;        // 系统公告
}




