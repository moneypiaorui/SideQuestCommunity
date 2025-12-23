package com.sidequest.core.interfaces.dto;

import com.sidequest.core.infrastructure.PostDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PostVO extends PostDO {
    private boolean hasLiked;
    private boolean hasFavorited;
}

