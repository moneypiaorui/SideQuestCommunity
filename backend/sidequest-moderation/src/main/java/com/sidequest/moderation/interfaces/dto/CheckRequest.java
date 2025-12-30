package com.sidequest.moderation.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CheckRequest {
    @NotBlank(message = "Content cannot be blank")
    private String content;
}




