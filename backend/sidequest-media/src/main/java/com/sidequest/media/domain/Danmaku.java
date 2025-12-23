package com.sidequest.media.domain;

import lombok.Builder;
import lombok.Getter;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Builder
public class Danmaku implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long videoId;
    private Long userId;
    private String content;
    private Long timeOffsetMs;
    private String color;
    private LocalDateTime createTime;
}

