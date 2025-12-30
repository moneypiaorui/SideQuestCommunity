package com.sidequest.media.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DanmakuRequest {
    @NotNull(message = "Video ID is required")
    private Long videoId;
    
    @NotBlank(message = "Content is required")
    private String content;
    
    @NotNull(message = "Time offset is required")
    private Long timeOffsetMs;
    
    private String color;
}




