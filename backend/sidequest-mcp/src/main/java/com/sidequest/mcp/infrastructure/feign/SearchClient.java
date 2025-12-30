package com.sidequest.mcp.infrastructure.feign;

import com.sidequest.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "search-service")
public interface SearchClient {
    @GetMapping("/api/search/posts")
    Result<Object> searchPosts(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    );
}




