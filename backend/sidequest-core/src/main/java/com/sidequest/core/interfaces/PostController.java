package com.sidequest.core.interfaces;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sidequest.common.Result;
import com.sidequest.common.context.UserContext;
import com.sidequest.core.application.PostService;
import com.sidequest.core.infrastructure.SectionDO;
import com.sidequest.core.infrastructure.TagDO;
import com.sidequest.core.interfaces.dto.CreatePostDTO;
import com.sidequest.core.interfaces.dto.PostVO;
import com.sidequest.core.interfaces.dto.UpdatePostDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Long authorId) {
        String userId = UserContext.getUserId();
        return Result.success(postService.getPostList(current, size, sectionId, tag, authorId, userId));
    }

    @GetMapping("/posts/following")
    public Result<Page<PostVO>> getFollowingPosts(
            @RequestParam(defaultValue = "1") @Min(1) int current,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        String userId = UserContext.getUserId();
        if (userId == null) return Result.error(401, "Unauthorized");
        return Result.success(postService.getFollowingPostList(current, size, userId));
    }

    @GetMapping("/posts/my")
    public Result<Page<PostVO>> getMyPosts(
            @RequestParam(defaultValue = "1") @Min(1) int current,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        String userId = UserContext.getUserId();
        if (userId == null) return Result.error(401, "Unauthorized");
        return Result.success(postService.getMyPosts(userId, current, size));
    }

    @GetMapping("/posts/recommended")
    public Result<Page<PostVO>> getRecommendedPosts(
            @RequestParam(defaultValue = "1") @Min(1) int current,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        String userId = UserContext.getUserId();
        return Result.success(postService.getRecommendedPosts(current, size, userId));
    }

    @GetMapping("/posts/{id}")
    public Result<PostVO> getPost(@PathVariable Long id) {
        String userId = UserContext.getUserId();
        PostVO post = postService.getPostDetail(id, userId);
        return post != null ? Result.success(post) : Result.error(404, "Post not found");
    }

    @PostMapping("/posts")
    public Result<Long> createPost(@Valid @RequestBody CreatePostDTO dto) {
        String userId = UserContext.getUserId();
        if (userId == null) return Result.error(401, "Unauthorized");
        Long postId = postService.handleCreatePost(userId, dto);
        return Result.success(postId);
    }

    @PutMapping("/posts/{id}")
    public Result<String> updatePost(@PathVariable Long id, @Valid @RequestBody UpdatePostDTO dto) {
        String userId = UserContext.getUserId();
        if (userId == null) return Result.error(401, "Unauthorized");
        postService.updatePost(id, Long.parseLong(userId), dto);
        return Result.success("Post updated");
    }

    @DeleteMapping("/posts/{id}")
    public Result<String> deletePost(@PathVariable Long id) {
        String userId = UserContext.getUserId();
        if (userId == null) return Result.error(401, "Unauthorized");
        postService.deletePostByUser(id, Long.parseLong(userId));
        return Result.success("Post deleted");
    }

    @GetMapping("/sections")
    public Result<List<SectionDO>> listSections() {
        return Result.success(postService.getAllSections());
    }

    @GetMapping("/sections/{id}")
    public Result<SectionDO> getSectionDetail(@PathVariable Long id) {
        SectionDO section = postService.getSectionDetail(id);
        return section != null ? Result.success(section) : Result.error(404, "Section not found");
    }

    @GetMapping("/tags/popular")
    public Result<List<TagDO>> listPopularTags() {
        return Result.success(postService.getPopularTags());
    }

    @GetMapping("/tags")
    public Result<List<TagDO>> listAllTags() {
        return Result.success(postService.getAllTags());
    }

    @PostMapping("/admin/posts/sync")
    public Result<String> syncPostsToSearch() {
        postService.syncAllPostsToSearch();
        return Result.success("Sync started");
    }
}
