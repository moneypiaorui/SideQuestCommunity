package com.sidequest.media.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_media")
public class MediaDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String fileName;
    private String fileKey;
    private String fileType; // image, video
    private String url;
    private Long authorId;
    private Integer status; // 0: PROCESSING, 1: READY, 2: FAILED
    private LocalDateTime createTime;

    public static final int STATUS_PROCESSING = 0;
    public static final int STATUS_READY = 1;
    public static final int STATUS_FAILED = 2;
}

