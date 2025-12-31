package com.sidequest.moderation.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ModerationCaseHandleRequest {
    @NotBlank
    private String result; // PASS/BLOCK/REVIEW
    private String reason;
}
