package com.sidequest.analytics.interfaces;

import com.sidequest.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/dashboard/stats")
    public Result<Map<String, Object>> getStats() {
        // Simple query from ClickHouse
        String sql = "SELECT count(*) as total_events FROM events";
        Map<String, Object> stats = jdbcTemplate.queryForMap(sql);
        return Result.success(stats);
    }

    @GetMapping("/dashboard/top-posts")
    public Result<List<Map<String, Object>>> getTopPosts() {
        String sql = "SELECT event_data, count(*) as count FROM events WHERE event_data LIKE '%post_view%' GROUP BY event_data ORDER BY count DESC LIMIT 10";
        return Result.success(jdbcTemplate.queryForList(sql));
    }
}

