package com.sidequest.mcp.infrastructure.feign;

import com.sidequest.common.Result;
import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "core-service")
public interface CoreClient {
    @PostMapping("/api/core/posts")
    Result<String> createPost(@RequestBody CreatePostDTO dto);

    @PostMapping("/api/core/interactions/comment")
    Result<String> addComment(@RequestBody CommentRequest request);

    @PostMapping("/api/core/interactions/like")
    Result<String> likePost(@RequestParam("postId") Long postId);

    @GetMapping("/api/core/sections")
    Result<List<Object>> listSections();

    @GetMapping("/api/core/tags/popular")
    Result<List<Object>> listPopularTags();

    @Data
    class CreatePostDTO {
        private String title;
        private String content;
        private Long sectionId;
    }

    @Data
    class CommentRequest {
        private Long postId;
        private String content;
    }
}

