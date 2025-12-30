package com.sidequest.identity.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PermissionMapper extends BaseMapper<PermissionDO> {
    default PermissionDO selectByCode(String code) {
        return selectOne(new LambdaQueryWrapper<PermissionDO>().eq(PermissionDO::getCode, code));
    }

    @Select("""
            SELECT DISTINCT p.code
            FROM t_permission p
            JOIN t_role_permission rp ON p.id = rp.permission_id
            JOIN t_user_role ur ON rp.role_id = ur.role_id
            WHERE ur.user_id = #{userId}
            """)
    List<String> selectCodesByUserId(@Param("userId") Long userId);
}
