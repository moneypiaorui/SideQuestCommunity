package com.sidequest.moderation.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CheckMediaRequest {
    @NotBlank
    private String url;
}
