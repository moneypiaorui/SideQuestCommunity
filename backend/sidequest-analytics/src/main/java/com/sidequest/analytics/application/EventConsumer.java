package com.sidequest.analytics.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventConsumer {
    private static final Pattern USER_ID_PATTERN = Pattern.compile("User\\s+(\\d+)");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "user-events", groupId = "analytics-group")
    public void processEvent(String eventJson, @Header(KafkaHeaders.RECEIVED_KEY) String eventType) {
        log.info("Analytics engine processing event: {}", eventJson);

        try {
            saveToClickHouse(eventJson, eventType);
        } catch (Exception e) {
            log.error("Failed to save event to ClickHouse", e);
        }
    }

    private void saveToClickHouse(String event, String eventType) {
        Long userId = null;
        String resolvedType = eventType;
        try {
            Map<String, Object> map = objectMapper.readValue(event, Map.class);
            Object uid = map.get("userId");
            if (uid != null) {
                userId = Long.valueOf(uid.toString());
            }
            if (resolvedType == null && map.get("type") != null) {
                resolvedType = map.get("type").toString();
            }
        } catch (Exception ignored) {
            Matcher matcher = USER_ID_PATTERN.matcher(event);
            if (matcher.find()) {
                userId = Long.valueOf(matcher.group(1));
            }
        }

        if (resolvedType == null || resolvedType.isBlank()) {
            resolvedType = "unknown";
        }

        String sql = "INSERT INTO events (event_type, user_id, event_time, event_data) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, resolvedType, userId, LocalDateTime.now(), event);
        log.debug("Data persisted to ClickHouse: type={}, userId={}", resolvedType, userId);
    }
}
