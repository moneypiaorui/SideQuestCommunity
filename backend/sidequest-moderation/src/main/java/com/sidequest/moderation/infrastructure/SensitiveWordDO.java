package com.sidequest.moderation.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_sensitive_word")
public class SensitiveWordDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String word;
    private String level; // S0/S1/S2
    private Integer status; // 1 enabled, 0 disabled
    private LocalDateTime createTime;
}
