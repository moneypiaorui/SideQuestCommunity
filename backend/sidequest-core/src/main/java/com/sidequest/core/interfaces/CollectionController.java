package com.sidequest.core.interfaces;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sidequest.common.Result;
import com.sidequest.common.context.UserContext;
import com.sidequest.core.application.PostService;
import com.sidequest.core.infrastructure.CollectionDO;
import com.sidequest.core.interfaces.dto.CollectionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/core/collections")
@RequiredArgsConstructor
public class CollectionController {
    private final PostService postService;

    @PostMapping
    public Result<CollectionDO> createCollection(@Valid @RequestBody CollectionRequest request) {
        String userId = UserContext.getUserId();
        if (userId == null) return Result.error(401, "Unauthorized");
        return Result.success(postService.createCollection(userId, request));
    }

    @GetMapping
    public Result<Page<CollectionDO>> listCollections(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        String userId = UserContext.getUserId();
        if (userId == null) return Result.error(401, "Unauthorized");
        return Result.success(postService.getCollections(userId, current, size));
    }

    @PutMapping("/{id}")
    public Result<CollectionDO> updateCollection(@PathVariable Long id, @Valid @RequestBody CollectionRequest request) {
        String userId = UserContext.getUserId();
        if (userId == null) return Result.error(401, "Unauthorized");
        return Result.success(postService.updateCollection(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public Result<String> deleteCollection(@PathVariable Long id) {
        String userId = UserContext.getUserId();
        if (userId == null) return Result.error(401, "Unauthorized");
        postService.deleteCollection(userId, id);
        return Result.success("Collection deleted");
    }

    @PostMapping("/{id}/items/{postId}")
    public Result<String> moveItem(@PathVariable Long id, @PathVariable Long postId) {
        String userId = UserContext.getUserId();
        if (userId == null) return Result.error(401, "Unauthorized");
        postService.moveFavoriteToCollection(userId, id, postId);
        return Result.success("Moved");
    }
}
