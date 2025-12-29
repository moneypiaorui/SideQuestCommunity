package com.sidequest.core.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_post_tag")
public class PostTagDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long postId;
    private Long tagId;
    private LocalDateTime createTime;
}
