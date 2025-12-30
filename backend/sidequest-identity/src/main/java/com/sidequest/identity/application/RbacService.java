package com.sidequest.identity.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sidequest.identity.infrastructure.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RbacService {
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;

    public List<RoleDO> listRoles() {
        return roleMapper.selectList(null);
    }

    public List<PermissionDO> listPermissions() {
        return permissionMapper.selectList(null);
    }

    public List<String> getUserRoles(Long userId) {
        return userRoleMapper.selectRoleCodesByUserId(userId);
    }

    public List<String> getUserPermissions(Long userId) {
        return permissionMapper.selectCodesByUserId(userId);
    }

    public void createRole(String code, String name, String description) {
        if (roleMapper.selectByCode(code) != null) {
            throw new RuntimeException("Role already exists");
        }
        RoleDO role = new RoleDO();
        role.setCode(code);
        role.setName(name);
        role.setDescription(description);
        role.setStatus(RoleDO.STATUS_ACTIVE);
        role.setCreateTime(LocalDateTime.now());
        roleMapper.insert(role);
    }

    public void createPermission(String code, String name, String description, String resource, String action) {
        if (permissionMapper.selectByCode(code) != null) {
            throw new RuntimeException("Permission already exists");
        }
        PermissionDO permission = new PermissionDO();
        permission.setCode(code);
        permission.setName(name);
        permission.setDescription(description);
        permission.setResource(resource);
        permission.setAction(action);
        permission.setCreateTime(LocalDateTime.now());
        permissionMapper.insert(permission);
    }

    @Transactional
    public void assignRolesToUser(Long userId, List<String> roleCodes) {
        if (roleCodes == null) {
            throw new RuntimeException("Role codes cannot be null");
        }
        List<RoleDO> roles = new ArrayList<>();
        for (String code : roleCodes) {
            RoleDO role = roleMapper.selectByCode(code);
            if (role == null) {
                throw new RuntimeException("Role not found: " + code);
            }
            roles.add(role);
        }

        userRoleMapper.delete(new LambdaQueryWrapper<UserRoleDO>().eq(UserRoleDO::getUserId, userId));
        for (RoleDO role : roles) {
            UserRoleDO userRole = new UserRoleDO();
            userRole.setUserId(userId);
            userRole.setRoleId(role.getId());
            userRole.setCreateTime(LocalDateTime.now());
            userRoleMapper.insert(userRole);
        }
    }

    @Transactional
    public void assignPermissionsToRole(String roleCode, List<String> permissionCodes) {
        if (permissionCodes == null) {
            throw new RuntimeException("Permission codes cannot be null");
        }
        RoleDO role = roleMapper.selectByCode(roleCode);
        if (role == null) {
            throw new RuntimeException("Role not found: " + roleCode);
        }

        List<PermissionDO> permissions = new ArrayList<>();
        for (String code : permissionCodes) {
            PermissionDO permission = permissionMapper.selectByCode(code);
            if (permission == null) {
                throw new RuntimeException("Permission not found: " + code);
            }
            permissions.add(permission);
        }

        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermissionDO>().eq(RolePermissionDO::getRoleId, role.getId()));
        for (PermissionDO permission : permissions) {
            RolePermissionDO rolePermission = new RolePermissionDO();
            rolePermission.setRoleId(role.getId());
            rolePermission.setPermissionId(permission.getId());
            rolePermission.setCreateTime(LocalDateTime.now());
            rolePermissionMapper.insert(rolePermission);
        }
    }
}
