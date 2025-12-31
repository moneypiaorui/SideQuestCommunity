package com.sidequest.identity.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenService {
    private static final String TOKEN_KEY_PREFIX = "auth:token:";
    private final StringRedisTemplate redisTemplate;

    public void storeToken(String token, long ttlMillis) {
        if (token == null || token.isBlank()) {
            return;
        }
        String key = TOKEN_KEY_PREFIX + token;
        redisTemplate.opsForValue().set(key, "1", Duration.ofMillis(ttlMillis));
    }

    public boolean isTokenActive(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String key = TOKEN_KEY_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void invalidateToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        String key = TOKEN_KEY_PREFIX + token;
        redisTemplate.delete(key);
    }
}
