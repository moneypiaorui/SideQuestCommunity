package com.sidequest.mcp.infrastructure.feign;

import com.sidequest.common.Result;
import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "core-service")
public interface CoreClient {
    @PostMapping("/api/core/posts")
    Result<String> createPost(@RequestBody CreatePostDTO dto);

    @PostMapping("/api/core/interactions/comment")
    Result<String> addComment(@RequestParam("postId") Long postId, @RequestBody String content);

    @Data
    class CreatePostDTO {
        private String title;
        private String content;
        private Long sectionId;
    }
}

