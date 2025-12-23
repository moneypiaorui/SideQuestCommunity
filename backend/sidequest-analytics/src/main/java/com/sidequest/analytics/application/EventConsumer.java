package com.sidequest.analytics.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventConsumer {
    
    private final JdbcTemplate jdbcTemplate;
    
    @KafkaListener(topics = "user-events", groupId = "analytics-group")
    public void processEvent(String eventJson) {
        log.info("Analytics engine processing event: {}", eventJson);
        
        try {
            saveToClickHouse(eventJson);
        } catch (Exception e) {
            log.error("Failed to save event to ClickHouse", e);
        }
    }

    private void saveToClickHouse(String event) {
        // 真正的 ClickHouse 持久化逻辑：将 JSON 事件打平并存入 ClickHouse
        String sql = "INSERT INTO events (event_data, created_at) VALUES (?, ?)";
        jdbcTemplate.update(sql, event, LocalDateTime.now());
        log.debug("Data persisted to ClickHouse: {}", event);
    }
}

