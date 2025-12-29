package com.sidequest.core.application;

import com.sidequest.core.domain.Post;
import com.sidequest.core.domain.service.PostDomainService;
import com.sidequest.core.infrastructure.CommentDO;
import com.sidequest.core.infrastructure.FavoriteDO;
import com.sidequest.core.infrastructure.PostDO;
import com.sidequest.core.infrastructure.LikeDO;
import com.sidequest.core.infrastructure.PostTagDO;
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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sidequest.common.Result;
import com.sidequest.common.event.UserEvent;

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
    private final PostTagMapper postTagMapper;
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
            TagDO tagDO = tagMapper.selectOne(new LambdaQueryWrapper<TagDO>().eq(TagDO::getName, tag));
            if (tagDO == null) {
                return new Page<>(current, size);
            }
            List<Long> postIds = postTagMapper.selectList(new LambdaQueryWrapper<PostTagDO>()
                    .eq(PostTagDO::getTagId, tagDO.getId()))
                    .stream()
                    .map(PostTagDO::getPostId)
                    .distinct()
                    .collect(Collectors.toList());
            if (postIds.isEmpty()) {
                return new Page<>(current, size);
            }
            queryWrapper.in(PostDO::getId, postIds);
        }
        queryWrapper.eq(PostDO::getStatus, PostDO.STATUS_NORMAL); 
        queryWrapper.orderByDesc(PostDO::getCreateTime);
        
        return getPostVOPage(current, size, currentUserId, queryWrapper);
    }

    public Page<PostVO> getFollowingPostList(int current, int size, String currentUserId) {
        if (currentUserId == null) return new Page<>(current, size);
        
        // 1. 鑾峰彇鍏虫敞鐨勭敤鎴峰垪琛?
        Result<List<Long>> followingRes = identityClient.getFollowingIds();
        if (followingRes.getCode() != 200 || followingRes.getData() == null || followingRes.getData().isEmpty()) {
            return new Page<>(current, size);
        }
        
        List<Long> followingIds = followingRes.getData();
        
        // 2. 鏌ヨ杩欎簺鐢ㄦ埛鐨勫笘瀛?
        Page<PostDO> page = new Page<>(current, size);
        LambdaQueryWrapper<PostDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(PostDO::getAuthorId, followingIds);
        queryWrapper.eq(PostDO::getStatus, PostDO.STATUS_NORMAL);
        queryWrapper.orderByDesc(PostDO::getCreateTime);
        
        return getPostVOPage(current, size, currentUserId, queryWrapper);
    }

    private Page<PostVO> getPostVOPage(int current, int size, String currentUserId, LambdaQueryWrapper<PostDO> queryWrapper) {
        Page<PostDO> postPage = postMapper.selectPage(new Page<>(current, size), queryWrapper);
        List<Long> postIds = postPage.getRecords().stream().map(PostDO::getId).collect(Collectors.toList());
        Map<Long, List<String>> tagsByPostId = getTagsByPostIds(postIds);
        
        // 鎻愬彇鎵€鏈変綔鑰?ID 杩涜鎵归噺鏌ヨ (妯℃嫙锛屽疄闄呭彲澧炲姞鎵归噺鎺ュ彛)
        Set<Long> authorIds = postPage.getRecords().stream().map(PostDO::getAuthorId).collect(Collectors.toSet());
        Map<Long, IdentityClient.UserDTO> userMap = authorIds.stream().collect(Collectors.toMap(
                id -> id,
                id -> {
                    try {
                        Result<IdentityClient.UserDTO> res = identityClient.getUserById(id);
                        return res.getData();
                    } catch (Exception e) {
                        return null;
                    }
                },
                (u1, u2) -> u1
        ));

        // 鑾峰彇褰撳墠鐢ㄦ埛鐨勫叧娉ㄥ垪琛?
        Set<Long> followingIds = new java.util.HashSet<>();
        if (currentUserId != null && !currentUserId.isBlank() && !"null".equals(currentUserId)) {
            try {
                Result<List<Long>> followingRes = identityClient.getFollowingIds();
                if (followingRes.getCode() == 200 && followingRes.getData() != null) {
                    followingIds.addAll(followingRes.getData());
                }
            } catch (Exception ignored) {}
        }

        Page<PostVO> voPage = new Page<>(current, size, postPage.getTotal());
        List<PostVO> voList = postPage.getRecords().stream().map(doItem -> {
            PostVO vo = convertToVO(doItem, currentUserId, tagsByPostId.get(doItem.getId()));
            IdentityClient.UserDTO user = userMap.get(doItem.getAuthorId());
            if (user != null) {
                vo.setAuthorAvatar(user.getAvatar());
                // 濡傛灉 PostDO 閲岀殑 authorName 涓虹┖鎴栨兂鐢ㄦ渶鏂扮殑锛屽彲浠ュ湪杩欓噷瑕嗙洊
                if (user.getNickname() != null) {
                    vo.setAuthorName(user.getNickname());
                }
            }
            vo.setFollowing(followingIds.contains(doItem.getAuthorId()));
            return vo;
        }).collect(Collectors.toList());
        
        voPage.setRecords(voList);
        return voPage;
    }

    public void syncAllPostsToSearch() {
        List<PostDO> allPosts = postMapper.selectList(new LambdaQueryWrapper<PostDO>()
                .eq(PostDO::getStatus, PostDO.STATUS_NORMAL));
        Map<Long, List<String>> tagsByPostId = getTagsByPostIds(
                allPosts.stream().map(PostDO::getId).collect(Collectors.toList())
        );
        
        for (PostDO postDO : allPosts) {
            try {
                String postJson = objectMapper.writeValueAsString(buildPostEvent(postDO, tagsByPostId.get(postDO.getId())));
                kafkaTemplate.send("post-topic", postDO.getId().toString(), postJson);
            } catch (Exception e) {
                log.error("Failed to resync post {}", postDO.getId(), e);
            }
        }
    }

    private PostVO convertToVO(PostDO doItem, String currentUserId, List<String> tags) {
        PostVO vo = new PostVO();
        BeanUtils.copyProperties(doItem, vo);
        
        // 澶勭悊鍥剧墖鍜屾爣绛剧殑杞崲
        if (doItem.getImageUrls() != null && !doItem.getImageUrls().isEmpty()) {
            vo.setImageUrls(List.of(doItem.getImageUrls().split(",")));
        }
        if (tags != null && !tags.isEmpty()) {
            vo.setTags(tags);
        }

        if (currentUserId != null && !currentUserId.isBlank() && !"null".equals(currentUserId)) {
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
        return tagMapper.selectList(
                new LambdaQueryWrapper<TagDO>()
                        .orderByDesc(TagDO::getHitCount)
                        .last("LIMIT 20")
        );
    }

    public PostVO getPostDetail(Long id, String currentUserId) {
        PostDO post = postMapper.selectById(id);
        if (post == null || post.getStatus() == PostDO.STATUS_DELETED) {
            return null;
        }
        // 澧炲姞闃呰鏁?
        postMapper.update(null, new LambdaUpdateWrapper<PostDO>()
                .eq(PostDO::getId, id)
                .setSql("view_count = view_count + 1"));
        post.setViewCount(post.getViewCount() + 1);
        
        List<String> tags = getTagsByPostIds(List.of(id)).getOrDefault(id, List.of());
        PostVO vo = convertToVO(post, currentUserId, tags);
        try {
            Result<IdentityClient.UserPublicDTO> userRes = identityClient.getUserPublicProfile(post.getAuthorId());
            if (userRes.getCode() == 200 && userRes.getData() != null) {
                vo.setAuthorAvatar(userRes.getData().getAvatar());
                vo.setAuthorName(userRes.getData().getNickname());
                vo.setFollowing(userRes.getData().isFollowing());
            }
        } catch (Exception ignored) {}
        
        return vo;
    }

    public List<CommentVO> getComments(Long postId) {
        LambdaQueryWrapper<CommentDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CommentDO::getPostId, postId);
        queryWrapper.orderByAsc(CommentDO::getCreateTime);
        List<CommentDO> comments = commentMapper.selectList(queryWrapper);
        
        if (comments.isEmpty()) return List.of();
        
        // 鑱氬悎鐢ㄦ埛淇℃伅
        Set<Long> userIds = comments.stream().map(CommentDO::getUserId).collect(Collectors.toSet());
        // 妯℃嫙鎵归噺鑾峰彇鐢ㄦ埛淇℃伅锛屽疄闄呬腑 IdentityClient 搴旀敮鎸佹壒閲忔帴鍙?
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

    public Page<PostVO> getUserFavoritePosts(String userId, int current, int size) {
        Long uid = Long.parseLong(userId);
        List<FavoriteDO> favorites = favoriteMapper.selectList(new LambdaQueryWrapper<FavoriteDO>()
                .eq(FavoriteDO::getUserId, uid)
                .orderByDesc(FavoriteDO::getCreateTime));
        
        if (favorites.isEmpty()) return new Page<>(current, size);
        
        List<Long> postIds = favorites.stream().map(FavoriteDO::getPostId).collect(Collectors.toList());
        LambdaQueryWrapper<PostDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(PostDO::getId, postIds);
        queryWrapper.eq(PostDO::getStatus, PostDO.STATUS_NORMAL);
        // PostgreSQL 涓嶆敮鎸?FIELD 鍑芥暟锛屼娇鐢?CASE WHEN 淇濇寔椤哄簭
        StringBuilder orderBy = new StringBuilder("CASE id ");
        for (int i = 0; i < postIds.size(); i++) {
            orderBy.append("WHEN ").append(postIds.get(i)).append(" THEN ").append(i).append(" ");
        }
        orderBy.append("END");
        queryWrapper.last("ORDER BY " + orderBy.toString());
        
        return getPostVOPage(current, size, userId, queryWrapper);
    }

    public Page<PostVO> getUserLikedPosts(String userId, int current, int size) {
        Long uid = Long.parseLong(userId);
        List<LikeDO> likes = likeMapper.selectList(new LambdaQueryWrapper<LikeDO>()
                .eq(LikeDO::getUserId, uid)
                .orderByDesc(LikeDO::getCreateTime));
        
        if (likes.isEmpty()) return new Page<>(current, size);
        
        List<Long> postIds = likes.stream().map(LikeDO::getPostId).collect(Collectors.toList());
        LambdaQueryWrapper<PostDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(PostDO::getId, postIds);
        queryWrapper.eq(PostDO::getStatus, PostDO.STATUS_NORMAL);
        
        // PostgreSQL 涓嶆敮鎸?FIELD 鍑芥暟锛屼娇鐢?CASE WHEN 淇濇寔椤哄簭
        StringBuilder orderBy = new StringBuilder("CASE id ");
        for (int i = 0; i < postIds.size(); i++) {
            orderBy.append("WHEN ").append(postIds.get(i)).append(" THEN ").append(i).append(" ");
        }
        orderBy.append("END");
        queryWrapper.last("ORDER BY " + orderBy.toString());
        
        return getPostVOPage(current, size, userId, queryWrapper);
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
            // 鍙戦€?Kafka 娑堟伅閫氱煡鎼滅储鏈嶅姟鍒犻櫎绱㈠紩
            kafkaTemplate.send("post-delete-topic", id.toString(), "Delete post " + id);
        }
    }

    public Page<PostDO> adminGetPostList(int current, int size, Integer status) {
        Page<PostDO> page = new Page<>(current, size);
        LambdaQueryWrapper<PostDO> queryWrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            queryWrapper.eq(PostDO::getStatus, status);
        }
        queryWrapper.orderByDesc(PostDO::getCreateTime);
        return postMapper.selectPage(page, queryWrapper);
    }

    @Transactional
    public void auditPost(Long id, boolean pass) {
        PostDO post = postMapper.selectById(id);
        if (post == null) {
            throw new RuntimeException("Post not found");
        }
        if (post.getStatus() != PostDO.STATUS_AUDITING) {
            throw new RuntimeException("Post is not in auditing status");
        }
        
        if (pass) {
            post.setStatus(PostDO.STATUS_NORMAL);
            // 瀹℃牳閫氳繃锛屽悓姝ュ埌鎼滅储鏈嶅姟
            try {
                String postJson = objectMapper.writeValueAsString(buildPostEvent(post, null));
                kafkaTemplate.send("post-topic", post.getId().toString(), postJson);
            } catch (Exception e) {
                log.error("Failed to send post creation event after audit", e);
            }
        } else {
            post.setStatus(PostDO.STATUS_BANNED);
        }
        post.setUpdateTime(LocalDateTime.now());
        postMapper.updateById(post);
    }

    @Transactional
    public Long handleCreatePost(String userId, CreatePostDTO dto) {
        // 0. 鏍￠獙鍒嗗尯鏄惁瀛樺湪
        if (dto.getSectionId() != null) {
            SectionDO section = sectionMapper.selectById(dto.getSectionId());
            if (section == null || section.getStatus() != 0) {
                throw new RuntimeException("Invalid section");
            }
        }

        // 1. 璋冪敤棰嗗煙鏈嶅姟杩涜鍐呭鏍￠獙
        if (!postDomainService.validateContent(dto.getContent())) {
            throw new RuntimeException("Content violates community rules");
        }

        // 3. 鏋勯€犻鍩熷璞″苟璋冪敤鍏朵笟鍔℃柟娉?
        Post post = Post.builder()
                .authorId(Long.parseLong(userId))
                .title(dto.getTitle())
                .content(dto.getContent())
                .sectionId(dto.getSectionId())
                .tags(dto.getTags())
                .imageUrls(dto.getImageUrls())
                .videoUrl(dto.getVideoUrl())
                .videoCoverUrl(dto.getVideoCoverUrl())
                .videoDuration(dto.getVideoDuration())
                .mediaId(dto.getMediaId())
                .build();
        
        post.publish(); 

        // 4. 鎸佷箙鍖?
        PostDO postDO = new PostDO();
        postDO.setAuthorId(post.getAuthorId());
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
        
        postDO.setVideoUrl(post.getVideoUrl());
        postDO.setVideoCoverUrl(post.getVideoCoverUrl());
        postDO.setVideoDuration(post.getVideoDuration());
        postDO.setMediaId(post.getMediaId());
        if (post.getImageUrls() != null) {
            postDO.setImageUrls(String.join(",", post.getImageUrls()));
        }
        
        postMapper.insert(postDO);
        savePostTags(postDO.getId(), dto.getTags());

        // 5. 鍙戦€佸紓姝ヤ簨浠?
        try {
            String postJson = objectMapper.writeValueAsString(buildPostEvent(postDO, normalizeTagNames(dto.getTags())));
            kafkaTemplate.send("post-topic", postDO.getId().toString(), postJson);
        } catch (Exception e) {
            log.error("Failed to send post creation event", e);
        }
        
        kafkaTemplate.send("user-events", "post_create", "User " + userId + " created post " + postDO.getId());

        return postDO.getId();
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
        
        // 鏇存柊甯栧瓙璇勮鏁?
        postMapper.update(null, new LambdaUpdateWrapper<PostDO>()
                .eq(PostDO::getId, postId)
                .setSql("comment_count = comment_count + 1"));
        
        try {
            kafkaTemplate.send("user-events", "post_comment", objectMapper.writeValueAsString(
                    UserEvent.builder()
                            .type("interaction")
                            .userId(Long.parseLong(userId))
                            .targetUserId(postMapper.selectById(postId).getAuthorId())
                            .targetId(postId)
                            .content("commented on your post")
                            .build()
            ));
        } catch (Exception e) {
            log.error("Failed to send comment event", e);
        }
    }

    @Transactional
    public void likePost(String userId, Long postId) {
        Long uid = Long.parseLong(userId);
        LambdaQueryWrapper<LikeDO> query = new LambdaQueryWrapper<LikeDO>()
                .eq(LikeDO::getPostId, postId)
                .eq(LikeDO::getUserId, uid);
        
        if (likeMapper.selectCount(query) > 0) {
            // 鍙栨秷鐐硅禐
            likeMapper.delete(query);
            postMapper.update(null, new LambdaUpdateWrapper<PostDO>()
                    .eq(PostDO::getId, postId)
                    .setSql("like_count = like_count - 1"));
        } else {
            // 鐐硅禐
            LikeDO likeDO = new LikeDO();
            likeDO.setPostId(postId);
            likeDO.setUserId(uid);
            likeDO.setCreateTime(LocalDateTime.now());
            likeMapper.insert(likeDO);
            postMapper.update(null, new LambdaUpdateWrapper<PostDO>()
                    .eq(PostDO::getId, postId)
                    .setSql("like_count = like_count + 1"));

            try {
                kafkaTemplate.send("user-events", "post_like", objectMapper.writeValueAsString(
                        UserEvent.builder()
                                .type("interaction")
                                .userId(uid)
                                .targetUserId(postMapper.selectById(postId).getAuthorId())
                                .targetId(postId)
                                .content("liked your post")
                                .build()
                ));
            } catch (Exception e) {
                log.error("Failed to send like event", e);
            }
        }
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

            try {
                kafkaTemplate.send("user-events", "post_favorite", objectMapper.writeValueAsString(
                        UserEvent.builder()
                                .type("interaction")
                                .userId(uid)
                                .targetUserId(postMapper.selectById(postId).getAuthorId())
                                .targetId(postId)
                                .content("favorited your post")
                                .build()
                ));
            } catch (Exception e) {
                log.error("Failed to send favorite event", e);
            }
        }
    }

    private List<String> normalizeTagNames(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        return tags.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .collect(Collectors.toList());
    }

    private void savePostTags(Long postId, List<String> tagNames) {
        List<String> normalized = normalizeTagNames(tagNames);
        if (normalized.isEmpty()) {
            return;
        }

        for (String name : normalized) {
            TagDO tag = tagMapper.selectOne(new LambdaQueryWrapper<TagDO>().eq(TagDO::getName, name));
            if (tag == null) {
                tag = new TagDO();
                tag.setName(name);
                tag.setHitCount(0L);
                tagMapper.insert(tag);
            }

            PostTagDO link = new PostTagDO();
            link.setPostId(postId);
            link.setTagId(tag.getId());
            link.setCreateTime(LocalDateTime.now());
            postTagMapper.insert(link);

            tagMapper.update(null, new LambdaUpdateWrapper<TagDO>()
                    .eq(TagDO::getId, tag.getId())
                    .setSql("hit_count = hit_count + 1"));
        }
    }

    private Map<Long, List<String>> getTagsByPostIds(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<PostTagDO> links = postTagMapper.selectList(
                new LambdaQueryWrapper<PostTagDO>().in(PostTagDO::getPostId, postIds)
        );
        if (links.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<Long> tagIds = links.stream().map(PostTagDO::getTagId).collect(Collectors.toSet());
        List<TagDO> tags = tagMapper.selectList(new LambdaQueryWrapper<TagDO>().in(TagDO::getId, tagIds));
        Map<Long, String> tagNameById = tags.stream().collect(Collectors.toMap(TagDO::getId, TagDO::getName));

        Map<Long, List<String>> result = new HashMap<>();
        for (PostTagDO link : links) {
            String tagName = tagNameById.get(link.getTagId());
            if (tagName == null) {
                continue;
            }
            result.computeIfAbsent(link.getPostId(), k -> new ArrayList<>()).add(tagName);
        }
        return result;
    }

    private Map<String, Object> buildPostEvent(PostDO post, List<String> tagNames) {
        Map<String, Object> postMap = objectMapper.convertValue(post, Map.class);
        List<String> tags = tagNames;
        if (tags == null) {
            tags = getTagsByPostIds(List.of(post.getId())).getOrDefault(post.getId(), List.of());
        }
        postMap.put("tags", String.join(",", tags));
        return postMap;
    }
}


