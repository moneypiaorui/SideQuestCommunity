package com.sidequest.identity.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_user")
public class UserDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String role; // USER, ADMIN
    private Integer status; // 0: NORMAL, 1: BANNED, 2: DELETED
    private Integer followerCount;
    private Integer followingCount;
    private Integer totalLikedCount;
    private Integer postCount;
    private java.time.LocalDateTime createTime;

    public static final int STATUS_NORMAL = 0;
    public static final int STATUS_BANNED = 1;
    public static final int STATUS_DELETED = 2;
}

