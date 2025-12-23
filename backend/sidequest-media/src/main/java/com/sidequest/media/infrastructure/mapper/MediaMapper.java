package com.sidequest.media.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sidequest.media.infrastructure.MediaDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MediaMapper extends BaseMapper<MediaDO> {
}

