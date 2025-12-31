package com.sidequest.search.interfaces.dto;

import lombok.Data;

@Data
public class UserSearchVO {
    private Long id;
    private String nickname;
    private String avatar;
    private Integer postCount;
}
