package com.sidequest.identity.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sidequest.identity.infrastructure.NotificationDO;
import com.sidequest.identity.infrastructure.NotificationMapper;
import com.sidequest.identity.interfaces.dto.UnreadCountVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final NotificationMapper notificationMapper;

    private static final String UNREAD_COUNT_KEY_PREFIX = "user:unread:count:";

    public UnreadCountVO getUnreadCount(Long userId) {
        String key = UNREAD_COUNT_KEY_PREFIX + userId;
        Map<Object, Object> counts = redisTemplate.opsForHash().entries(key);
        
        return UnreadCountVO.builder()
                .chat(getIntValue(counts.get("chat")))
                .interaction(getIntValue(counts.get("interaction")))
                .system(getIntValue(counts.get("system")))
                .build();
    }

    private int getIntValue(Object value) {
        if (value == null) return 0;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void markAsRead(Long userId, String type) {
        String key = UNREAD_COUNT_KEY_PREFIX + userId;
        redisTemplate.opsForHash().put(key, type, "0");

        LambdaQueryWrapper<NotificationDO> query = new LambdaQueryWrapper<NotificationDO>()
                .eq(NotificationDO::getUserId, userId)
                .eq(NotificationDO::getType, type)
                .eq(NotificationDO::getStatus, NotificationDO.STATUS_UNREAD);
        NotificationDO update = new NotificationDO();
        update.setStatus(NotificationDO.STATUS_READ);
        notificationMapper.update(update, query);
    }

    public Page<NotificationDO> listNotifications(Long userId, String type, Integer status, int current, int size) {
        Page<NotificationDO> page = new Page<>(current, size);
        LambdaQueryWrapper<NotificationDO> query = new LambdaQueryWrapper<NotificationDO>()
                .eq(NotificationDO::getUserId, userId)
                .orderByDesc(NotificationDO::getCreateTime);
        if (type != null && !type.isBlank()) {
            query.eq(NotificationDO::getType, type);
        }
        if (status != null) {
            query.eq(NotificationDO::getStatus, status);
        }
        return notificationMapper.selectPage(page, query);
    }

    @KafkaListener(topics = "user-events", groupId = "identity-notification-group")
    public void handleUserEvents(String message) {
        log.info("Received user event for notification: {}", message);
        try {
            JsonNode node = objectMapper.readTree(message);
            String type = node.get("type").asText();
            Long targetUserId = node.get("targetUserId").asLong();
            
            if ("interaction".equals(type)) {
                createNotification(targetUserId, "interaction", node.get("content").asText(""));
                incrementUnreadCount(targetUserId, "interaction");
            } else if ("system".equals(type)) {
                createNotification(targetUserId, "system", node.get("content").asText(""));
                incrementUnreadCount(targetUserId, "system");
            }
        } catch (Exception e) {
            log.warn("Failed to process user event as JSON, might be old format: {}", message);
        }
    }

    @KafkaListener(topics = "chat-message-topic", groupId = "identity-notification-group")
    public void handleChatMessages(String message) {
        log.info("Received chat message for notification: {}", message);
        try {
            JsonNode node = objectMapper.readTree(message);
            // ChatMessageDO has senderId and roomId
            // For now, we don't have a direct targetUserId in ChatMessageDO.
            // In a real system, we would query the room members.
            // Here, we'll assume the message should notify the other member(s) in the room.
            // Since this is identity-service, it might not have access to chat tables.
            // A better way is for chat-service to emit a 'new_message' event with targetUserId.
        } catch (Exception e) {
            log.error("Failed to process chat message for notification", e);
        }
    }

    // 增加计数的方法，供内部或消费者调用
    public void incrementUnreadCount(Long userId, String type) {
        String key = UNREAD_COUNT_KEY_PREFIX + userId;
        redisTemplate.opsForHash().increment(key, type, 1);
    }

    private void createNotification(Long userId, String type, String content) {
        NotificationDO notification = new NotificationDO();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setContent(content);
        notification.setStatus(NotificationDO.STATUS_UNREAD);
        notificationMapper.insert(notification);
    }
}

