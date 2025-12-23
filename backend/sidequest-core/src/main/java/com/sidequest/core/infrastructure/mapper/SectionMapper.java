package com.sidequest.core.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sidequest.core.infrastructure.SectionDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SectionMapper extends BaseMapper<SectionDO> {
}

