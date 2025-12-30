package com.sidequest.identity.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_role")
public class RoleDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private String description;
    private Integer status;
    private java.time.LocalDateTime createTime;

    public static final int STATUS_ACTIVE = 0;
    public static final int STATUS_DISABLED = 1;
}
