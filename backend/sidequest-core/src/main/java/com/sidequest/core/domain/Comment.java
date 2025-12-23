package com.sidequest.core.domain;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class Comment {
    private Long id;
    private Long postId;
    private Long userId;
    private String content;
    private Long parentId;
    private LocalDateTime createTime;
}

