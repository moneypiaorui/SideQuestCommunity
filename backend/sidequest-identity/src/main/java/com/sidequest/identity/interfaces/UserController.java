package com.sidequest.identity.interfaces;

import com.sidequest.common.Result;
import com.sidequest.common.context.UserContext;
import com.sidequest.identity.application.UserService;
import com.sidequest.identity.infrastructure.UserDO;
import com.sidequest.identity.interfaces.dto.UserPublicDTO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/identity")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public Result<UserDO> getCurrentUser() {
        String userId = UserContext.getUserId();
        UserDO user = userService.getUserById(Long.parseLong(userId));
        return user != null ? Result.success(user) : Result.error(404, "User not found");
    }

    @GetMapping("/users/{id}/public")
    public Result<UserPublicDTO> getPublicProfile(@PathVariable Long id) {
        UserDO user = userService.getUserById(id);
        if (user == null) return Result.error(404, "User not found");
        
        UserPublicDTO dto = new UserPublicDTO();
        dto.setId(user.getId());
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
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
    public Result<Page<UserDO>> listUsers(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        return Result.success(userService.getUserList(current, size, status));
    }

    @GetMapping("/users/{id}")
    public Result<UserDO> getUserById(@PathVariable Long id) {
        UserDO user = userService.getUserById(id);
        return user != null ? Result.success(user) : Result.error(404, "User not found");
    }

    @PutMapping("/profile")
    public Result<String> updateProfile(@RequestBody UpdateProfileRequest request) {
        String userId = UserContext.getUserId();
        userService.updateProfile(Long.parseLong(userId), request.getNickname(), request.getAvatar());
        return Result.success("Profile updated");
    }

    @PostMapping("/admin/users/{id}/ban")
    public Result<String> banUser(@PathVariable Long id) {
        // In a real app, check for admin role here
        userService.banUser(id);
        return Result.success("User banned");
    }

    @Data
    public static class UpdateProfileRequest {
        private String nickname;
        private String avatar;
    }
}

