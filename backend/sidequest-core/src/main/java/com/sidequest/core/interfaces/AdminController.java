package com.sidequest.core.interfaces;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sidequest.common.Result;
import com.sidequest.core.application.PostService;
import com.sidequest.core.infrastructure.PostDO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/core/admin/posts")
@RequiredArgsConstructor
public class AdminController {
    private final PostService postService;

    @GetMapping
    public Result<Page<PostDO>> getPostList(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        return Result.success(postService.adminGetPostList(current, size, status));
    }

    @PostMapping("/{id}/audit")
    public Result<String> auditPost(@PathVariable Long id, @RequestParam boolean pass) {
        postService.auditPost(id, pass);
        return Result.success(pass ? "Post approved" : "Post rejected");
    }

    @DeleteMapping("/{id}")
    public Result<String> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return Result.success("Post deleted");
    }

    @PostMapping("/{id}/pin")
    public Result<String> pinPost(@PathVariable Long id, @RequestParam boolean pinned) {
        postService.pinPost(id, pinned);
        return Result.success(pinned ? "Post pinned" : "Post unpinned");
    }

    @PostMapping("/{id}/feature")
    public Result<String> featurePost(@PathVariable Long id, @RequestParam boolean featured) {
        postService.featurePost(id, featured);
        return Result.success(featured ? "Post featured" : "Post unfeatured");
    }
}

