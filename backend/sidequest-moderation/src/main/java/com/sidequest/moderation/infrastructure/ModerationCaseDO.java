package com.sidequest.moderation.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_moderation_case")
public class ModerationCaseDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String contentType; // text/image/video
    private String content;
    private String contentUrl;
    private String result; // PASS/BLOCK/REVIEW
    private String level; // S0/S1/S2
    private String reason;
    private Integer status; // 0 pending, 1 handled
    private Long operatorId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
