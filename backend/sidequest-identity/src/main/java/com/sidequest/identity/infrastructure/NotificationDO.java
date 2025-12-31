package com.sidequest.identity.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_notification")
public class NotificationDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String type; // chat/interaction/system
    private String content;
    private Integer status; // 0: unread, 1: read
    private LocalDateTime createTime;

    public static final int STATUS_UNREAD = 0;
    public static final int STATUS_READ = 1;
}
