package com.sidequest.core.interfaces;

import com.sidequest.common.Result;
import com.sidequest.common.context.UserContext;
import com.sidequest.core.application.PostService;
import com.sidequest.core.interfaces.dto.CommentRequest;
import com.sidequest.core.interfaces.dto.CommentVO;
import com.sidequest.core.interfaces.dto.PostVO;
import com.sidequest.core.interfaces.dto.RatingRequest;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/core/interactions")
@RequiredArgsConstructor
public class InteractionController {
    private final PostService postService;

    @GetMapping("/comments")
    public Result<List<CommentVO>> getComments(@RequestParam Long postId) {
        return Result.success(postService.getComments(postId));
    }

    @GetMapping("/comments/{id}/replies")
    public Result<List<CommentVO>> getReplies(@PathVariable Long id) {
        return Result.success(postService.getReplies(id));
    }

    @GetMapping("/favorites")
    public Result<Page<PostVO>> getMyFavorites(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        String userId = UserContext.getUserId();
        if (userId == null) return Result.error(401, "Unauthorized");
        return Result.success(postService.getUserFavoritePosts(userId, current, size));
    }

    @GetMapping("/likes")
    public Result<Page<PostVO>> getMyLikes(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        String userId = UserContext.getUserId();
        if (userId == null) return Result.error(401, "Unauthorized");
        return Result.success(postService.getUserLikedPosts(userId, current, size));
    }
    
    @PostMapping("/comment")
    public Result<String> addComment(@Valid @RequestBody CommentRequest request) {
        String userId = UserContext.getUserId();
        if (userId == null) return Result.error(401, "Unauthorized");
        postService.addComment(userId, request.getPostId(), request.getContent());
        return Result.success("Comment added");
    }

    @DeleteMapping("/comments/{id}")
    public Result<String> deleteComment(@PathVariable Long id) {
        String userId = UserContext.getUserId();
        if (userId == null) return Result.error(401, "Unauthorized");
        postService.deleteComment(userId, id);
        return Result.success("Comment deleted");
    }

    @PostMapping("/like")
    public Result<String> likePost(@RequestParam Long postId) {
        String userId = UserContext.getUserId();
        if (userId == null) return Result.error(401, "Unauthorized");
        postService.likePost(userId, postId);
        return Result.success("Operation successful");
    }

    @PostMapping("/favorite")
    public Result<String> favoritePost(@RequestParam Long postId, @RequestParam(required = false) Long collectionId) {
        String userId = UserContext.getUserId();
        if (userId == null) return Result.error(401, "Unauthorized");
        postService.favoritePost(userId, postId, collectionId);
        return Result.success("Favorited successfully");
    }

    @PostMapping("/rating")
    public Result<String> ratePost(@Valid @RequestBody RatingRequest request) {
        String userId = UserContext.getUserId();
        if (userId == null) return Result.error(401, "Unauthorized");
        postService.ratePost(userId, request.getPostId(), request.getScore());
        return Result.success("Rated successfully");
    }
}

