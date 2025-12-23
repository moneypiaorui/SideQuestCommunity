package com.sidequest.chat.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_chat_room")
public class ChatRoomDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String type; // PRIVATE, GROUP
    private LocalDateTime createTime;
}

@Data
@TableName("t_chat_message")
public class ChatMessageDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private Long senderId;
    private String content;
    private String type; // TEXT, IMAGE, etc.
    private Integer status; // 0: UNREAD, 1: READ
    private LocalDateTime createTime;
}

