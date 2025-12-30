package com.sidequest.identity.interfaces;

import com.sidequest.common.Result;
import com.sidequest.identity.application.RbacService;
import com.sidequest.identity.infrastructure.PermissionDO;
import com.sidequest.identity.infrastructure.RoleDO;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/identity/admin")
@RequiredArgsConstructor
public class RbacController {
    private final RbacService rbacService;

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_LIST')")
    public Result<List<RoleDTO>> listRoles() {
        List<RoleDTO> roles = rbacService.listRoles().stream()
                .map(RoleDTO::from)
                .collect(Collectors.toList());
        return Result.success(roles);
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('PERMISSION_LIST')")
    public Result<List<PermissionDTO>> listPermissions() {
        List<PermissionDTO> permissions = rbacService.listPermissions().stream()
                .map(PermissionDTO::from)
                .collect(Collectors.toList());
        return Result.success(permissions);
    }

    @GetMapping("/users/{id}/roles")
    @PreAuthorize("hasAuthority('USER_ROLE_ASSIGN')")
    public Result<List<String>> getUserRoles(@PathVariable Long id) {
        return Result.success(rbacService.getUserRoles(id));
    }

    @GetMapping("/users/{id}/permissions")
    @PreAuthorize("hasAuthority('USER_ROLE_ASSIGN')")
    public Result<List<String>> getUserPermissions(@PathVariable Long id) {
        return Result.success(rbacService.getUserPermissions(id));
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    public Result<String> createRole(@RequestBody CreateRoleRequest request) {
        rbacService.createRole(request.getCode(), request.getName(), request.getDescription());
        return Result.success("Role created");
    }

    @PostMapping("/permissions")
    @PreAuthorize("hasAuthority('PERMISSION_CREATE')")
    public Result<String> createPermission(@RequestBody CreatePermissionRequest request) {
        rbacService.createPermission(
                request.getCode(),
                request.getName(),
                request.getDescription(),
                request.getResource(),
                request.getAction()
        );
        return Result.success("Permission created");
    }

    @PostMapping("/users/{id}/roles")
    @PreAuthorize("hasAuthority('USER_ROLE_ASSIGN')")
    public Result<String> assignRoles(@PathVariable Long id, @RequestBody AssignRolesRequest request) {
        rbacService.assignRolesToUser(id, request.getRoleCodes());
        return Result.success("Roles assigned");
    }

    @PostMapping("/roles/{code}/permissions")
    @PreAuthorize("hasAuthority('ROLE_PERMISSION_ASSIGN')")
    public Result<String> assignPermissions(@PathVariable String code, @RequestBody AssignPermissionsRequest request) {
        rbacService.assignPermissionsToRole(code, request.getPermissionCodes());
        return Result.success("Permissions assigned");
    }

    @Data
    public static class CreateRoleRequest {
        @NotBlank
        private String code;
        @NotBlank
        private String name;
        private String description;
    }

    @Data
    public static class CreatePermissionRequest {
        @NotBlank
        private String code;
        @NotBlank
        private String name;
        private String description;
        private String resource;
        private String action;
    }

    @Data
    public static class AssignRolesRequest {
        private List<String> roleCodes;
    }

    @Data
    public static class AssignPermissionsRequest {
        private List<String> permissionCodes;
    }

    @Data
    public static class RoleDTO {
        private String code;
        private String name;
        private String description;
        private Integer status;

        public static RoleDTO from(RoleDO role) {
            RoleDTO dto = new RoleDTO();
            dto.setCode(role.getCode());
            dto.setName(role.getName());
            dto.setDescription(role.getDescription());
            dto.setStatus(role.getStatus());
            return dto;
        }
    }

    @Data
    public static class PermissionDTO {
        private String code;
        private String name;
        private String description;
        private String resource;
        private String action;

        public static PermissionDTO from(PermissionDO permission) {
            PermissionDTO dto = new PermissionDTO();
            dto.setCode(permission.getCode());
            dto.setName(permission.getName());
            dto.setDescription(permission.getDescription());
            dto.setResource(permission.getResource());
            dto.setAction(permission.getAction());
            return dto;
        }
    }
}
