package com.sidequest.moderation.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sidequest.moderation.infrastructure.ModerationCaseDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ModerationCaseMapper extends BaseMapper<ModerationCaseDO> {
}
