package com.sidequest.core.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sidequest.core.infrastructure.RatingDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RatingMapper extends BaseMapper<RatingDO> {
}
