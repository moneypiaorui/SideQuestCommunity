package com.sidequest.identity.interfaces;

import com.sidequest.common.Result;
import com.sidequest.common.context.UserContext;
import com.sidequest.identity.application.UserService;
import com.sidequest.identity.infrastructure.UserDO;
import com.sidequest.identity.interfaces.dto.UserPublicDTO;
import com.sidequest.identity.interfaces.dto.UserVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/identity")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public Result<UserVO> getCurrentUser() {
        String userId = UserContext.getUserId();
        UserDO user = userService.getUserById(Long.parseLong(userId));
        return user != null ? Result.success(convertToVO(user)) : Result.error(404, "User not found");
    }

    @GetMapping("/users/{id}/public")
    public Result<UserPublicDTO> getPublicProfile(@PathVariable Long id) {
        UserDO user = userService.getUserById(id);
        if (user == null) return Result.error(404, "User not found");
        
        UserPublicDTO dto = new UserPublicDTO();
        dto.setId(user.getId());
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
        dto.setSignature(user.getSignature());
        dto.setRole(user.getRole());
        dto.setFollowerCount(user.getFollowerCount());
        dto.setFollowingCount(user.getFollowingCount());
        dto.setTotalLikedCount(user.getTotalLikedCount());
        dto.setPostCount(user.getPostCount());
        
        String currentUserId = UserContext.getUserId();
        if (currentUserId != null) {
            dto.setFollowing(userService.isFollowing(Long.parseLong(currentUserId), id));
        }
        
        return Result.success(dto);
    }

    @PostMapping("/users/{id}/follow")
    public Result<String> follow(@PathVariable Long id) {
        String currentUserId = UserContext.getUserId();
        if (currentUserId == null) return Result.error(401, "Unauthorized");
        userService.followUser(Long.parseLong(currentUserId), id);
        return Result.success("Followed");
    }

    @PostMapping("/users/{id}/unfollow")
    public Result<String> unfollow(@PathVariable Long id) {
        String currentUserId = UserContext.getUserId();
        if (currentUserId == null) return Result.error(401, "Unauthorized");
        userService.unfollowUser(Long.parseLong(currentUserId), id);
        return Result.success("Unfollowed");
    }

    @GetMapping("/admin/users")
    @PreAuthorize("hasAuthority('USER_LIST')")
    public Result<Page<UserVO>> listUsers(
            @RequestParam(defaultValue = "1") @Min(1) int current,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) Integer status) {
        Page<UserDO> userPage = userService.getUserList(current, size, status);
        Page<UserVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        List<UserVO> voList = userPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return Result.success(voPage);
    }

    @GetMapping("/users/{id}")
    public Result<UserVO> getUserById(@PathVariable Long id) {
        UserDO user = userService.getUserById(id);
        return user != null ? Result.success(convertToVO(user)) : Result.error(404, "User not found");
    }

    @PutMapping("/profile")
    public Result<String> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        String userId = UserContext.getUserId();
        userService.updateProfile(Long.parseLong(userId), request.getNickname(), request.getAvatar(), request.getSignature());
        return Result.success("Profile updated");
    }

    @PutMapping("/password")
    public Result<String> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        String userId = UserContext.getUserId();
        userService.updatePassword(Long.parseLong(userId), request.getOldPassword(), request.getNewPassword());
        return Result.success("Password updated");
    }

    @GetMapping("/me/following-ids")
    public Result<List<Long>> getFollowingIds() {
        String userId = UserContext.getUserId();
        return Result.success(userService.getFollowingIds(Long.parseLong(userId)));
    }

    @GetMapping("/users/{id}/followers")
    public Result<Page<UserVO>> getFollowers(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        Page<UserDO> userPage = userService.getFollowers(id, current, size);
        Page<UserVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        voPage.setRecords(userPage.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return Result.success(voPage);
    }

    @GetMapping("/users/{id}/following")
    public Result<Page<UserVO>> getFollowing(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        Page<UserDO> userPage = userService.getFollowings(id, current, size);
        Page<UserVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        voPage.setRecords(userPage.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return Result.success(voPage);
    }

    @GetMapping("/users/{id}/posts")
    public Result<Object> getUserPosts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(userService.getUserPosts(id, current, size));
    }

    @PostMapping("/admin/users/{id}/ban")
    @PreAuthorize("hasAuthority('USER_BAN')")
    public Result<String> banUser(@PathVariable Long id, @RequestParam(required = false) String reason) {
        String operatorId = UserContext.getUserId();
        userService.banUser(id, operatorId == null ? null : Long.parseLong(operatorId), reason);
        return Result.success("User banned");
    }

    @PostMapping("/admin/users/{id}/unban")
    @PreAuthorize("hasAuthority('USER_BAN')")
    public Result<String> unbanUser(@PathVariable Long id) {
        String operatorId = UserContext.getUserId();
        userService.unbanUser(id, operatorId == null ? null : Long.parseLong(operatorId));
        return Result.success("User unbanned");
    }

    @DeleteMapping("/admin/users/{id}")
    @PreAuthorize("hasAuthority('USER_BAN')")
    public Result<String> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success("User deleted");
    }

    private UserVO convertToVO(UserDO user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }

    @Data
    public static class UpdateProfileRequest {
        @NotBlank(message = "Nickname cannot be blank")
        private String nickname;
        private String avatar;
        private String signature;
    }

    @Data
    public static class UpdatePasswordRequest {
        @NotBlank(message = "Old password cannot be blank")
        private String oldPassword;
        @NotBlank(message = "New password cannot be blank")
        private String newPassword;
    }
}

