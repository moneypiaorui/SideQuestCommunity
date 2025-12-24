package com.sidequest.identity.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class User {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String signature;
    private String role;
    private Integer status; // 0: Normal, 1: BANNED, 2: DELETED
    private Integer followerCount;
    private Integer followingCount;
    private Integer totalLikedCount;
    private Integer postCount;

    public void ban() {
        this.status = 1;
    }

    public void unban() {
        this.status = 0;
    }
}

