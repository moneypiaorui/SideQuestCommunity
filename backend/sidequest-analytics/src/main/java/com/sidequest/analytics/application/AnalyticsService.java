package com.sidequest.analytics.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {
    private final JdbcTemplate jdbcTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String STATS_CACHE_KEY = "analytics:dashboard:stats";
    private static final String TOP_POSTS_CACHE_KEY = "analytics:dashboard:top_posts";
    private static final String USER_STATS_CACHE_KEY_PREFIX = "analytics:user:stats:";

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> cached = (Map<String, Object>) redisTemplate.opsForValue().get(STATS_CACHE_KEY);
        if (cached != null) return cached;

        String sql = "SELECT count(*) as total_events FROM events";
        Map<String, Object> stats = jdbcTemplate.queryForMap(sql);
        
        // Add more stats
        stats.put("total_users", jdbcTemplate.queryForObject("SELECT count(DISTINCT user_id) FROM events", Long.class));
        stats.put("active_users_24h", jdbcTemplate.queryForObject("SELECT count(DISTINCT user_id) FROM events WHERE event_time > now() - INTERVAL 1 DAY", Long.class));

        redisTemplate.opsForValue().set(STATS_CACHE_KEY, stats, Duration.ofMinutes(10));
        return stats;
    }

    public List<Map<String, Object>> getTopPosts() {
        List<Map<String, Object>> cached = (List<Map<String, Object>>) redisTemplate.opsForValue().get(TOP_POSTS_CACHE_KEY);
        if (cached != null) return cached;

        String sql = "SELECT event_data as post_id, count(*) as view_count " +
                    "FROM events " +
                    "WHERE event_type = 'post_view' " +
                    "GROUP BY event_data " +
                    "ORDER BY view_count DESC " +
                    "LIMIT 10";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);

        redisTemplate.opsForValue().set(TOP_POSTS_CACHE_KEY, result, Duration.ofMinutes(30));
        return result;
    }

    public Map<String, Object> getUserStats(Long userId) {
        String key = USER_STATS_CACHE_KEY_PREFIX + userId;
        Map<String, Object> cached = (Map<String, Object>) redisTemplate.opsForValue().get(key);
        if (cached != null) return cached;

        String sql = "SELECT count(*) as total_actions FROM events WHERE user_id = ?";
        Map<String, Object> stats = jdbcTemplate.queryForMap(sql, userId);
        
        redisTemplate.opsForValue().set(key, stats, Duration.ofHours(1));
        return stats;
    }
}




