package com.sidequest.core.interfaces.dto;

import lombok.Data;

@Data
public class RatingRequest {
    private Long postId;
    private Integer score;
}
