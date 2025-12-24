package com.sidequest.identity.interfaces.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String signature;
    private String role;
    private Integer status;
    private Integer followerCount;
    private Integer followingCount;
    private Integer totalLikedCount;
    private Integer postCount;
    private LocalDateTime createTime;
}

