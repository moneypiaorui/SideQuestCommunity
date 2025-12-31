package com.sidequest.search.interfaces;

import com.sidequest.common.Result;
import com.sidequest.search.domain.PostRepository;
import com.sidequest.search.infrastructure.PostDoc;
import com.sidequest.search.infrastructure.feign.IdentityClient;
import com.sidequest.search.interfaces.dto.UserSearchVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {
    private final PostRepository postRepository;
    private final IdentityClient identityClient;

    @GetMapping("/posts")
    public Result<Page<PostDoc>> searchPosts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        // 修复搜索逻辑：仅对标题和标签进行搜索，且大小写不敏感
        Page<PostDoc> postDocs = postRepository.findByKeyword(keyword, pageable);
        postDocs.forEach(this::enrichAuthorInfo);
        return Result.success(postDocs);
    }

    @GetMapping("/posts/advanced")
    public Result<Page<PostDoc>> searchPostsAdvanced(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Long fromTime,
            @RequestParam(required = false) Long toTime,
            @RequestParam(required = false) Boolean hasVideo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<PostDoc> basePage;
        if (keyword != null && !keyword.isBlank()) {
            basePage = postRepository.findByKeyword(keyword, pageable);
        } else if (sectionId != null) {
            basePage = postRepository.findBySectionIdAndStatus(sectionId, 1, pageable);
        } else {
            basePage = postRepository.findByStatus(1, pageable);
        }

        List<PostDoc> docs = basePage.getContent().stream()
                .filter(doc -> sectionId == null || Objects.equals(doc.getSectionId(), sectionId))
                .filter(doc -> tag == null || tag.isBlank() || (doc.getTags() != null && doc.getTags().contains(tag)))
                .filter(doc -> fromTime == null || (doc.getCreateTime() != null && doc.getCreateTime() >= fromTime))
                .filter(doc -> toTime == null || (doc.getCreateTime() != null && doc.getCreateTime() <= toTime))
                .filter(doc -> hasVideo == null || (hasVideo ? doc.getVideoUrl() != null && !doc.getVideoUrl().isBlank()
                        : doc.getVideoUrl() == null || doc.getVideoUrl().isBlank()))
                .collect(Collectors.toList());
        docs.forEach(this::enrichAuthorInfo);

        Page<PostDoc> resultPage = new org.springframework.data.domain.PageImpl<>(
                docs, pageable, docs.size());
        return Result.success(resultPage);
    }

    @GetMapping("/user/posts")
    public Result<Page<PostDoc>> searchUserPosts(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<PostDoc> postDocs = postRepository.findByAuthorIdAndStatus(userId, 1, pageable);
        postDocs.forEach(this::enrichAuthorInfo);
        return Result.success(postDocs);
    }

    @GetMapping("/users")
    public Result<List<UserSearchVO>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PostDoc> postDocs = postRepository.findByAuthorNameKeyword(keyword, pageable);

        Map<Long, UserSearchVO> users = new LinkedHashMap<>();
        for (PostDoc doc : postDocs.getContent()) {
            if (doc.getAuthorId() == null) {
                continue;
            }
            UserSearchVO user = users.computeIfAbsent(doc.getAuthorId(), id -> {
                UserSearchVO vo = new UserSearchVO();
                vo.setId(id);
                vo.setNickname(doc.getAuthorName());
                vo.setAvatar(doc.getAuthorAvatar());
                vo.setPostCount(0);
                return vo;
            });
            user.setPostCount(user.getPostCount() + 1);
        }

        return Result.success(new ArrayList<>(users.values()));
    }

    @GetMapping("/tags")
    public Result<List<String>> searchTags(@RequestParam String keyword,
                                           @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(0, Math.max(size, 1));
        Page<PostDoc> postDocs = postRepository.findByKeyword(keyword, pageable);
        List<String> tags = postDocs.getContent().stream()
                .filter(doc -> doc.getTags() != null)
                .flatMap(doc -> doc.getTags().stream())
                .filter(Objects::nonNull)
                .filter(tag -> tag.contains(keyword))
                .distinct()
                .limit(size)
                .collect(Collectors.toList());
        return Result.success(tags);
    }

    private void enrichAuthorInfo(PostDoc doc) {
        if (doc.getAuthorId() != null) {
            try {
                Result<IdentityClient.UserDTO> userRes = identityClient.getUserById(doc.getAuthorId());
                if (userRes.getCode() == 200 && userRes.getData() != null) {
                    doc.setAuthorName(userRes.getData().getNickname());
                    doc.setAuthorAvatar(userRes.getData().getAvatar());
                }
            } catch (Exception ignored) {
                // 如果身份服务不可用，保留原始数据
            }
        }
    }
}

