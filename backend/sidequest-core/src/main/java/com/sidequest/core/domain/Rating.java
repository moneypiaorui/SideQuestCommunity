package com.sidequest.core.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Rating {
    private Long id;
    private Long postId;
    private Long userId;
    private Integer score; // 1-5
}

