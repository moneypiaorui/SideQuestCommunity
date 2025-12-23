package com.sidequest.core.application;

import com.sidequest.core.infrastructure.CommentDO;
import com.sidequest.core.infrastructure.FavoriteDO;
import com.sidequest.core.infrastructure.PostDO;
import com.sidequest.core.infrastructure.LikeDO;
import com.sidequest.core.infrastructure.SectionDO;
import com.sidequest.core.infrastructure.TagDO;
import com.sidequest.core.infrastructure.feign.IdentityClient;
import com.sidequest.core.infrastructure.mapper.*;
import com.sidequest.core.interfaces.dto.CommentVO;
import com.sidequest.core.interfaces.dto.CreatePostDTO;
import com.sidequest.core.interfaces.dto.PostVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final LikeMapper likeMapper;
    private final FavoriteMapper favoriteMapper;
    private final SectionMapper sectionMapper;
    private final TagMapper tagMapper;
    private final PostDomainService postDomainService;
    private final IdentityClient identityClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public Page<PostVO> getPostList(int current, int size, Long sectionId, String tag, String currentUserId) {
        Page<PostDO> page = new Page<>(current, size);
        LambdaQueryWrapper<PostDO> queryWrapper = new LambdaQueryWrapper<>();
        if (sectionId != null) {
            queryWrapper.eq(PostDO::getSectionId, sectionId);
        }
        if (tag != null && !tag.isBlank()) {
            queryWrapper.like(PostDO::getTags, tag);
        }
        queryWrapper.eq(PostDO::getStatus, PostDO.STATUS_NORMAL); 
        queryWrapper.orderByDesc(PostDO::getCreateTime);
        
        Page<PostDO> postPage = postMapper.selectPage(page, queryWrapper);
        
        Page<PostVO> voPage = new Page<>(current, size, postPage.getTotal());
        List<PostVO> voList = postPage.getRecords().stream().map(doItem -> {
            PostVO vo = convertToVO(doItem, currentUserId);
            return vo;
        }).collect(Collectors.toList());
        
        voPage.setRecords(voList);
        return voPage;
    }

    private PostVO convertToVO(PostDO doItem, String currentUserId) {
        PostVO vo = new PostVO();
        BeanUtils.copyProperties(doItem, vo);
        if (currentUserId != null) {
            Long uid = Long.parseLong(currentUserId);
            vo.setHasLiked(likeMapper.selectCount(new LambdaQueryWrapper<LikeDO>().eq(LikeDO::getPostId, doItem.getId()).eq(LikeDO::getUserId, uid)) > 0);
            vo.setHasFavorited(favoriteMapper.selectCount(new LambdaQueryWrapper<FavoriteDO>().eq(FavoriteDO::getPostId, doItem.getId()).eq(FavoriteDO::getUserId, uid)) > 0);
        }
        return vo;
    }

    public List<SectionDO> getAllSections() {
        return sectionMapper.selectList(new LambdaQueryWrapper<SectionDO>().eq(SectionDO::getStatus, 0));
    }

    public List<TagDO> getPopularTags() {
        return tagMapper.selectList(new LambdaQueryWrapper<TagDO>().orderByDesc(TagDO::getHitCount).last("LIMIT 20"));
    }

    public PostVO getPostDetail(Long id, String currentUserId) {
        PostDO post = postMapper.selectById(id);
        if (post == null || post.getStatus() == PostDO.STATUS_DELETED) {
            return null;
        }
        // 增加阅读数
        postMapper.update(null, new LambdaUpdateWrapper<PostDO>()
                .eq(PostDO::getId, id)
                .setSql("view_count = view_count + 1"));
        post.setViewCount(post.getViewCount() + 1);
        
        return convertToVO(post, currentUserId);
    }

    public List<CommentVO> getComments(Long postId) {
        LambdaQueryWrapper<CommentDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CommentDO::getPostId, postId);
        queryWrapper.orderByAsc(CommentDO::getCreateTime);
        List<CommentDO> comments = commentMapper.selectList(queryWrapper);
        
        if (comments.isEmpty()) return List.of();
        
        // 聚合用户信息
        Set<Long> userIds = comments.stream().map(CommentDO::getUserId).collect(Collectors.toSet());
        // 模拟批量获取用户信息，实际中 IdentityClient 应支持批量接口
        Map<Long, IdentityClient.UserDTO> userMap = userIds.stream().collect(Collectors.toMap(
                id -> id,
                id -> {
                    try {
                        Result<IdentityClient.UserDTO> res = identityClient.getUserById(id);
                        return res.getData();
                    } catch (Exception e) {
                        return null;
                    }
                }
        ));
        
        return comments.stream().map(c -> {
            CommentVO vo = new CommentVO();
            BeanUtils.copyProperties(c, vo);
            IdentityClient.UserDTO user = userMap.get(c.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar()); 
            }
            return vo;
        }).collect(Collectors.toList());
    }

    public List<FavoriteDO> getUserFavorites(String userId) {
        LambdaQueryWrapper<FavoriteDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FavoriteDO::getUserId, Long.parseLong(userId));
        return favoriteMapper.selectList(queryWrapper);
    }

    @Transactional
    public void deletePost(Long id) {
        PostDO post = postMapper.selectById(id);
        if (post != null) {
            post.setStatus(PostDO.STATUS_DELETED);
            postMapper.updateById(post);
            // 发送 Kafka 消息通知搜索服务删除索引
            kafkaTemplate.send("post-delete-topic", id.toString(), "Delete post " + id);
        }
    }

    @Transactional
    public void handleCreatePost(String userId, CreatePostDTO dto) {
        // 0. 校验分区是否存在
        if (dto.getSectionId() != null) {
            SectionDO section = sectionMapper.selectById(dto.getSectionId());
            if (section == null || section.getStatus() != 0) {
                throw new RuntimeException("Invalid section");
            }
        }

        // 1. 调用领域服务进行内容校验
        if (!postDomainService.validateContent(dto.getContent())) {
            throw new RuntimeException("Content violates community rules");
        }

        // 2. 获取真实的作者昵称
        String nickname = "Unknown";
        try {
            Result<IdentityClient.UserDTO> userResult = identityClient.getUserById(Long.parseLong(userId));
            if (userResult.getCode() == 200 && userResult.getData() != null) {
                nickname = userResult.getData().getNickname();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch user nickname for userId: {}", userId, e);
        }

        // 3. 构造领域对象并调用其业务方法
        Post post = Post.builder()
                .authorId(Long.parseLong(userId))
                .title(dto.getTitle())
                .content(dto.getContent())
                .sectionId(dto.getSectionId())
                .tags(dto.getTags())
                .build();
        
        post.publish(); 

        // 4. 持久化
        PostDO postDO = new PostDO();
        postDO.setAuthorId(post.getAuthorId());
        postDO.setAuthorName(nickname);
        postDO.setTitle(post.getTitle());
        postDO.setContent(post.getContent());
        postDO.setSectionId(post.getSectionId());
        postDO.setStatus(PostDO.STATUS_AUDITING); 
        postDO.setLikeCount(0);
        postDO.setCommentCount(0);
        postDO.setFavoriteCount(0);
        postDO.setViewCount(0);
        postDO.setCreateTime(post.getCreateTime());
        postDO.setUpdateTime(LocalDateTime.now());
        
        if (post.getTags() != null) {
            postDO.setTags(String.join(",", post.getTags()));
        }
        postDO.setVideoUrl(post.getVideoUrl());
        if (post.getImageUrls() != null) {
            postDO.setImageUrls(String.join(",", post.getImageUrls()));
        }
        
        postMapper.insert(postDO);

        // 5. 发送异步事件
        try {
            String postJson = objectMapper.writeValueAsString(postDO);
            kafkaTemplate.send("post-topic", postDO.getId().toString(), postJson);
        } catch (Exception e) {
            log.error("Failed to send post creation event", e);
        }
        
        kafkaTemplate.send("user-events", "post_create", "User " + userId + " created post " + postDO.getId());
    }

    @Transactional
    public void addComment(String userId, Long postId, String content) {
        if (!postDomainService.validateContent(content)) {
            throw new RuntimeException("Comment content violates rules");
        }
        
        CommentDO commentDO = new CommentDO();
        commentDO.setUserId(Long.parseLong(userId));
        commentDO.setPostId(postId);
        commentDO.setContent(content);
        commentDO.setCreateTime(LocalDateTime.now());
        commentMapper.insert(commentDO);
        
        // 更新帖子评论数
        postMapper.update(null, new LambdaUpdateWrapper<PostDO>()
                .eq(PostDO::getId, postId)
                .setSql("comment_count = comment_count + 1"));
        
        kafkaTemplate.send("user-events", "post_comment", "User " + userId + " commented on " + postId);
    }

    @Transactional
    public void likePost(String userId, Long postId) {
        Long uid = Long.parseLong(userId);
        LambdaQueryWrapper<LikeDO> query = new LambdaQueryWrapper<LikeDO>()
                .eq(LikeDO::getPostId, postId)
                .eq(LikeDO::getUserId, uid);
        
        if (likeMapper.selectCount(query) > 0) {
            // 取消点赞
            likeMapper.delete(query);
            postMapper.update(null, new LambdaUpdateWrapper<PostDO>()
                    .eq(PostDO::getId, postId)
                    .setSql("like_count = like_count - 1"));
        } else {
            // 点赞
            LikeDO likeDO = new LikeDO();
            likeDO.setPostId(postId);
            likeDO.setUserId(uid);
            likeDO.setCreateTime(LocalDateTime.now());
            likeMapper.insert(likeDO);
            postMapper.update(null, new LambdaUpdateWrapper<PostDO>()
                    .eq(PostDO::getId, postId)
                    .setSql("like_count = like_count + 1"));
        }
        
        kafkaTemplate.send("user-events", "post_like", "User " + userId + " liked " + postId);
    }

    @Transactional
    public void favoritePost(String userId, Long postId, Long collectionId) {
        Long uid = Long.parseLong(userId);
        LambdaQueryWrapper<FavoriteDO> query = new LambdaQueryWrapper<FavoriteDO>()
                .eq(FavoriteDO::getPostId, postId)
                .eq(FavoriteDO::getUserId, uid);
        
        if (favoriteMapper.selectCount(query) > 0) {
            favoriteMapper.delete(query);
            postMapper.update(null, new LambdaUpdateWrapper<PostDO>()
                    .eq(PostDO::getId, postId)
                    .setSql("favorite_count = favorite_count - 1"));
        } else {
            FavoriteDO favoriteDO = new FavoriteDO();
            favoriteDO.setUserId(uid);
            favoriteDO.setPostId(postId);
            favoriteDO.setCollectionId(collectionId);
            favoriteDO.setCreateTime(LocalDateTime.now());
            favoriteMapper.insert(favoriteDO);
            postMapper.update(null, new LambdaUpdateWrapper<PostDO>()
                    .eq(PostDO::getId, postId)
                    .setSql("favorite_count = favorite_count + 1"));
        }
        
        kafkaTemplate.send("user-events", "post_favorite", "User " + userId + " favorited " + postId);
    }
}
