package com.sidequest.core.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentRequest {
    @NotNull(message = "Post ID is required")
    private Long postId;
    
    @NotBlank(message = "Comment content cannot be blank")
    private String content;
}




