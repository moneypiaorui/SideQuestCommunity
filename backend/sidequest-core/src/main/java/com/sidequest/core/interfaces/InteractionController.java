package com.sidequest.core.interfaces;

import com.sidequest.common.Result;
import com.sidequest.common.context.UserContext;
import com.sidequest.core.application.PostService;
import com.sidequest.core.infrastructure.CommentDO;
import com.sidequest.core.infrastructure.FavoriteDO;
import com.sidequest.core.interfaces.dto.CommentVO;
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

    @GetMapping("/favorites")
    public Result<List<FavoriteDO>> getMyFavorites() {
        String userId = UserContext.getUserId();
        return Result.success(postService.getUserFavorites(userId));
    }
    
    @PostMapping("/comment")
    public Result<String> addComment(@RequestParam Long postId, @RequestBody String content) {
        String userId = UserContext.getUserId();
        postService.addComment(userId, postId, content);
        return Result.success("Comment added");
    }

    @PostMapping("/like")
    public Result<String> likePost(@RequestParam Long postId) {
        String userId = UserContext.getUserId();
        postService.likePost(userId, postId);
        return Result.success("Operation successful");
    }

    @PostMapping("/favorite")
    public Result<String> favoritePost(@RequestParam Long postId, @RequestParam(required = false) Long collectionId) {
        String userId = UserContext.getUserId();
        postService.favoritePost(userId, postId, collectionId);
        return Result.success("Favorited successfully");
    }
}

