package com.sidequest.chat.infrastructure.feign;

import com.sidequest.common.Result;
import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "identity-service")
public interface IdentityClient {
    @GetMapping("/api/identity/users/{id}")
    Result<UserDTO> getUserById(@PathVariable("id") Long id);

    @Data
    class UserDTO {
        private Long id;
        private String nickname;
        private String avatar;
    }
}

