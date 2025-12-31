package com.sidequest.media.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MediaCompleteRequest {
    @NotBlank
    private String fileName;
    @NotBlank
    private String fileKey;
    @NotBlank
    private String fileType; // image, video
    private String url;
}
