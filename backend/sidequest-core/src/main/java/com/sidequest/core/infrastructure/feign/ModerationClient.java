package com.sidequest.core.infrastructure.feign;

import com.sidequest.common.Result;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "moderation-service")
public interface ModerationClient {
    
    @PostMapping("/api/moderation/check")
    Result<Boolean> checkText(@RequestBody CheckRequest request);

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    class CheckRequest {
        private String content;
    }
}
