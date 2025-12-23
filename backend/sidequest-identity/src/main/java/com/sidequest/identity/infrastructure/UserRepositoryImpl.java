package com.sidequest.identity.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sidequest.identity.domain.User;
import com.sidequest.identity.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final UserMapper userMapper;

    @Override
    public void save(User user) {
        UserDO userDO = new UserDO();
        userDO.setId(user.getId());
        userDO.setUsername(user.getUsername());
        userDO.setPassword(user.getPassword());
        userDO.setNickname(user.getNickname());
        userDO.setAvatar(user.getAvatar());
        userDO.setRole(user.getRole());
        userDO.setStatus(user.getStatus());
        userDO.setFollowerCount(user.getFollowerCount());
        userDO.setFollowingCount(user.getFollowingCount());
        userDO.setTotalLikedCount(user.getTotalLikedCount());
        userDO.setPostCount(user.getPostCount());
        
        if (user.getId() == null) {
            userMapper.insert(userDO);
        } else {
            userMapper.updateById(userDO);
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        UserDO userDO = userMapper.selectOne(new LambdaQueryWrapper<UserDO>().eq(UserDO::getUsername, username));
        return Optional.ofNullable(toDomain(userDO));
    }

    @Override
    public Optional<User> findById(Long id) {
        UserDO userDO = userMapper.selectById(id);
        return Optional.ofNullable(toDomain(userDO));
    }

    private User toDomain(UserDO userDO) {
        if (userDO == null) return null;
        return User.builder()
                .id(userDO.getId())
                .username(userDO.getUsername())
                .password(userDO.getPassword())
                .nickname(userDO.getNickname())
                .avatar(userDO.getAvatar())
                .role(userDO.getRole())
                .status(userDO.getStatus())
                .followerCount(userDO.getFollowerCount())
                .followingCount(userDO.getFollowingCount())
                .totalLikedCount(userDO.getTotalLikedCount())
                .postCount(userDO.getPostCount())
                .build();
    }
}

