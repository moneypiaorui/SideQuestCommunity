package com.sidequest.core.interfaces;

import com.sidequest.common.Result;
import com.sidequest.common.context.UserContext;
import com.sidequest.core.application.PostService;
import com.sidequest.core.domain.Post;
import com.sidequest.core.infrastructure.PostDO;
import com.sidequest.core.interfaces.dto.CreatePostDTO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.sidequest.core.infrastructure.PostDO;
import com.sidequest.core.infrastructure.SectionDO;
import com.sidequest.core.infrastructure.TagDO;
import com.sidequest.core.interfaces.dto.CreatePostDTO;
import com.sidequest.core.interfaces.dto.PostVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/core")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;

    @GetMapping("/posts")
    public Result<Page<PostVO>> listPosts(
            @RequestParam(defaultValue = "1") @Min(1) int current,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) String tag) {
        String userId = UserContext.getUserId();
        return Result.success(postService.getPostList(current, size, sectionId, tag, userId));
    }

    @GetMapping("/posts/following")
    public Result<Page<PostVO>> getFollowingPosts(
            @RequestParam(defaultValue = "1") @Min(1) int current,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        String userId = UserContext.getUserId();
        if (userId == null) return Result.error(401, "Unauthorized");
        return Result.success(postService.getFollowingPostList(current, size, userId));
    }

    @GetMapping("/sections")
    public Result<List<SectionDO>> listSections() {
        return Result.success(postService.getAllSections());
    }

    @GetMapping("/tags/popular")
    public Result<List<TagDO>> listPopularTags() {
        return Result.success(postService.getPopularTags());
    }

    @GetMapping("/posts/{id}")
    public Result<PostVO> getPost(@PathVariable Long id) {
        String userId = UserContext.getUserId();
        PostVO post = postService.getPostDetail(id, userId);
        return post != null ? Result.success(post) : Result.error(404, "Post not found");
    }

    @PostMapping("/posts")
    public Result<String> createPost(@Valid @RequestBody CreatePostDTO dto) {
        String userId = UserContext.getUserId();
        if (userId == null) return Result.error(401, "Unauthorized");
        
        postService.handleCreatePost(userId, dto);
        return Result.success("Post created successfully");
    }

    @PostMapping("/admin/posts/sync")
    public Result<String> syncPostsToSearch() {
        postService.syncAllPostsToSearch();
        return Result.success("Sync started");
    }
}
