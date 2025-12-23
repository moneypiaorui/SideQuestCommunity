package com.sidequest.chat.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sidequest.chat.infrastructure.ChatRoomDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatRoomMapper extends BaseMapper<ChatRoomDO> {
}

