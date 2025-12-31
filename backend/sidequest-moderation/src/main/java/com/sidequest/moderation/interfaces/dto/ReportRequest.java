package com.sidequest.moderation.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportRequest {
    @NotBlank
    private String targetType; // post/comment/user
    @NotNull
    private Long targetId;
    @NotBlank
    private String reason;
}
