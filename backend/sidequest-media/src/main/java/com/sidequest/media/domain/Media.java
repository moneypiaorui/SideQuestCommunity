package com.sidequest.media.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Media {
    private Long id;
    private String fileName;
    private String fileKey;
    private String fileType; // image/video
    private String url;
    private Long authorId;
    private Integer status; // 0: Processing, 1: Ready, 2: Failed
}

