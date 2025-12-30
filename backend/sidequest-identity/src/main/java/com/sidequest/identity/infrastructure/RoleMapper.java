package com.sidequest.identity.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoleMapper extends BaseMapper<RoleDO> {
    default RoleDO selectByCode(String code) {
        return selectOne(new LambdaQueryWrapper<RoleDO>().eq(RoleDO::getCode, code));
    }
}
