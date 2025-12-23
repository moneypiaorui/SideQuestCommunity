package com.sidequest.core.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_section")
public class SectionDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String displayNameZh;
    private String displayNameEn;
    private Integer status; // 0: Normal, 1: Hidden
}

