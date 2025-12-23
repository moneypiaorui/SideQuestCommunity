package com.sidequest.core.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Favorite {
    private Long id;
    private Long postId;
    private Long userId;
    private Long collectionId;
}

