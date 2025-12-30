package com.sidequest.identity.application;

import com.sidequest.identity.domain.User;
import com.sidequest.identity.domain.UserRepository;
import com.sidequest.identity.infrastructure.JwtUtils;
import com.sidequest.identity.domain.User;
import com.sidequest.identity.domain.UserRepository;
import com.sidequest.identity.infrastructure.*;
import com.sidequest.identity.interfaces.dto.LoginVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final FollowMapper followMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PermissionMapper permissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public Page<UserDO> getUserList(int current, int size, Integer status) {
        Page<UserDO> page = new Page<>(current, size);
        LambdaQueryWrapper<UserDO> queryWrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            queryWrapper.eq(UserDO::getStatus, status);
        }
        return userMapper.selectPage(page, queryWrapper);
    }

    public UserDO getUserById(Long userId) {
        return userMapper.selectById(userId);
    }

    public void updateProfile(Long userId, String nickname, String avatar, String signature) {
        UserDO userDO = userMapper.selectById(userId);
        if (userDO != null) {
            userDO.setNickname(nickname);
            userDO.setAvatar(avatar);
            if (signature != null) {
                if (signature.length() > 50) {
                    throw new RuntimeException("Signature too long (max 50 characters)");
                }
                userDO.setSignature(signature);
            }
            userMapper.updateById(userDO);
        }
    }

    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        UserDO userDO = userMapper.selectById(userId);
        if (userDO == null) {
            throw new RuntimeException("User not found");
        }
        
        if (!passwordEncoder.matches(oldPassword, userDO.getPassword())) {
            throw new RuntimeException("Invalid old password");
        }
        
        userDO.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(userDO);
    }

    public void banUser(Long userId) {
        UserDO userDO = userMapper.selectById(userId);
        if (userDO != null) {
            userDO.setStatus(UserDO.STATUS_BANNED);
            userMapper.updateById(userDO);
        }
    }

    public void register(String username, String password, String nickname) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .nickname(nickname)
                .role("USER")
                .status(UserDO.STATUS_NORMAL)
                .followerCount(0)
                .followingCount(0)
                .totalLikedCount(0)
                .postCount(0)
                .build();
                
        userRepository.save(user);

        UserDO savedUser = userMapper.selectByUsername(username);
        if (savedUser == null) {
            throw new RuntimeException("Failed to load user after registration");
        }
        RoleDO defaultRole = roleMapper.selectByCode("USER");
        if (defaultRole == null) {
            throw new RuntimeException("Default role USER not found");
        }
        UserRoleDO userRole = new UserRoleDO();
        userRole.setUserId(savedUser.getId());
        userRole.setRoleId(defaultRole.getId());
        userRole.setCreateTime(LocalDateTime.now());
        userRoleMapper.insert(userRole);
    }

    @Transactional
    public void followUser(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new RuntimeException("You cannot follow yourself");
        }
        
        LambdaQueryWrapper<FollowDO> query = new LambdaQueryWrapper<FollowDO>()
                .eq(FollowDO::getFollowerId, followerId)
                .eq(FollowDO::getFollowingId, followingId);
        
        if (followMapper.selectCount(query) == 0) {
            FollowDO follow = new FollowDO();
            follow.setFollowerId(followerId);
            follow.setFollowingId(followingId);
            follow.setCreateTime(LocalDateTime.now());
            followMapper.insert(follow);
            
            // 更新计数
            userMapper.update(null, new LambdaUpdateWrapper<UserDO>()
                    .eq(UserDO::getId, followerId)
                    .setSql("following_count = following_count + 1"));
            userMapper.update(null, new LambdaUpdateWrapper<UserDO>()
                    .eq(UserDO::getId, followingId)
                    .setSql("follower_count = follower_count + 1"));
        }
    }

    public Page<UserDO> getFollowers(Long userId, int current, int size) {
        Page<FollowDO> followPage = new Page<>(current, size);
        followMapper.selectPage(followPage, new LambdaQueryWrapper<FollowDO>()
                .eq(FollowDO::getFollowingId, userId)
                .orderByDesc(FollowDO::getCreateTime));
        
        List<Long> followerIds = followPage.getRecords().stream()
                .map(FollowDO::getFollowerId)
                .collect(java.util.stream.Collectors.toList());
        
        Page<UserDO> userPage = new Page<>(followPage.getCurrent(), followPage.getSize(), followPage.getTotal());
        if (!followerIds.isEmpty()) {
            userPage.setRecords(userMapper.selectBatchIds(followerIds));
        }
        return userPage;
    }

    public Page<UserDO> getFollowings(Long userId, int current, int size) {
        Page<FollowDO> followPage = new Page<>(current, size);
        followMapper.selectPage(followPage, new LambdaQueryWrapper<FollowDO>()
                .eq(FollowDO::getFollowerId, userId)
                .orderByDesc(FollowDO::getCreateTime));
        
        List<Long> followingIds = followPage.getRecords().stream()
                .map(FollowDO::getFollowingId)
                .collect(java.util.stream.Collectors.toList());
        
        Page<UserDO> userPage = new Page<>(followPage.getCurrent(), followPage.getSize(), followPage.getTotal());
        if (!followingIds.isEmpty()) {
            userPage.setRecords(userMapper.selectBatchIds(followingIds));
        }
        return userPage;
    }

    @Transactional
    public void unfollowUser(Long followerId, Long followingId) {
        LambdaQueryWrapper<FollowDO> query = new LambdaQueryWrapper<FollowDO>()
                .eq(FollowDO::getFollowerId, followerId)
                .eq(FollowDO::getFollowingId, followingId);
        
        if (followMapper.selectCount(query) > 0) {
            followMapper.delete(query);
            userMapper.update(null, new LambdaUpdateWrapper<UserDO>()
                    .eq(UserDO::getId, followerId)
                    .setSql("following_count = following_count - 1"));
            userMapper.update(null, new LambdaUpdateWrapper<UserDO>()
                    .eq(UserDO::getId, followingId)
                    .setSql("follower_count = follower_count - 1"));
        }
    }

    public boolean isFollowing(Long followerId, Long followingId) {
        return followMapper.selectCount(new LambdaQueryWrapper<FollowDO>()
                .eq(FollowDO::getFollowerId, followerId)
                .eq(FollowDO::getFollowingId, followingId)) > 0;
    }

    public List<Long> getFollowingIds(Long userId) {
        return followMapper.selectList(new LambdaQueryWrapper<FollowDO>()
                .eq(FollowDO::getFollowerId, userId))
                .stream().map(FollowDO::getFollowingId).collect(java.util.stream.Collectors.toList());
    }

    public LoginVO login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }
        
        if (user.getStatus() == UserDO.STATUS_BANNED) {
            throw new RuntimeException("User is banned");
        }
        
        if (user.getStatus() == UserDO.STATUS_DELETED) {
            throw new RuntimeException("User account deleted");
        }
        
        List<String> roles = userRoleMapper.selectRoleCodesByUserId(user.getId());
        if (roles == null || roles.isEmpty()) {
            roles = new java.util.ArrayList<>();
            if (user.getRole() != null) {
                roles.add(user.getRole());
            }
        }
        List<String> permissions = permissionMapper.selectCodesByUserId(user.getId());
        if (permissions == null) {
            permissions = new java.util.ArrayList<>();
        }
        String primaryRole = resolvePrimaryRole(roles);

        String token = jwtUtils.generateToken(user.getId().toString(), primaryRole, roles, permissions);
        return LoginVO.builder()
                .token(token)
                .expireIn(86400L) // 假设 24 小时
                .userId(user.getId())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .roles(roles)
                .permissions(permissions)
                .build();
    }

    private String resolvePrimaryRole(List<String> roles) {
        if (roles != null && roles.contains("ADMIN")) {
            return "ADMIN";
        }
        if (roles != null && !roles.isEmpty()) {
            return roles.get(0);
        }
        return "USER";
    }
}

