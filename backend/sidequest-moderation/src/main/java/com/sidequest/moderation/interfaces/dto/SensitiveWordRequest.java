package com.sidequest.moderation.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SensitiveWordRequest {
    @NotBlank
    private String word;
    private String level; // S0/S1/S2
}
