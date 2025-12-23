package com.sidequest.core.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_post")
public class PostDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long authorId;
    private String authorName; // 冗余字段，优化查询
    private String title;
    private String content;
    private Long sectionId;
    private Integer status; // 0: AUDITING, 1: NORMAL, 2: BANNED, 3: DELETED
    private Integer likeCount;
    private Integer commentCount;
    private Integer favoriteCount;
    private Integer viewCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    private String imageUrls; 
    private String videoUrl;
    private String tags;

    public static final int STATUS_AUDITING = 0;
    public static final int STATUS_NORMAL = 1;
    public static final int STATUS_BANNED = 2;
    public static final int STATUS_DELETED = 3;
}
