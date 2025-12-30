package com.sidequest.media.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DanmakuVO implements Serializable {
    private Long videoId;
    private Long userId;
    private String content;
    private Long timeOffsetMs;
    private String color;
}




