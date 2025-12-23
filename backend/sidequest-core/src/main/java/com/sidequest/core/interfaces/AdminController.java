package com.sidequest.core.interfaces;

import com.sidequest.common.Result;
import com.sidequest.core.application.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
public class AdminController {
    private final PostService postService;

    @DeleteMapping("/{id}")
    public Result<String> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return Result.success("Post deleted");
    }
}

