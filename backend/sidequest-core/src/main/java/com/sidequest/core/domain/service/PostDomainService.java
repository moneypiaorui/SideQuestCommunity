package com.sidequest.core.domain.service;

import com.sidequest.common.Result;
import com.sidequest.core.infrastructure.feign.ModerationClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostDomainService {
    private final ModerationClient moderationClient;

    public boolean validateContent(String content) {
        Result<Boolean> result = moderationClient.checkText(new ModerationClient.CheckRequest(content));
        return result != null && Boolean.TRUE.equals(result.getData());
    }
}

