package com.sidequest.core.interfaces.dto;

import com.sidequest.core.infrastructure.CommentDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CommentVO extends CommentDO {
    private String nickname;
    private String avatar;
}

