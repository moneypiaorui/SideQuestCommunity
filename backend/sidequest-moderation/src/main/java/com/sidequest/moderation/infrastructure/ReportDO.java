package com.sidequest.moderation.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_report")
public class ReportDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String targetType; // post/comment/user
    private Long targetId;
    private Long reporterId;
    private String reason;
    private Integer status; // 0 pending, 1 handled, 2 rejected
    private Long handlerId;
    private String handleResult;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
