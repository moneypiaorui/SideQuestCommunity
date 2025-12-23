package com.sidequest.core.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_tag")
public class TagDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long hit_count;
}

