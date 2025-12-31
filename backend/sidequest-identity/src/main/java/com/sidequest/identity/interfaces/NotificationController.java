package com.sidequest.identity.interfaces;

import com.sidequest.common.Result;
import com.sidequest.common.context.UserContext;
import com.sidequest.identity.application.NotificationService;
import com.sidequest.identity.infrastructure.NotificationDO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sidequest.identity.interfaces.dto.UnreadCountVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping("/unread-count")
    public Result<UnreadCountVO> getUnreadCount() {
        String userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "Unauthorized");
        }
        return Result.success(notificationService.getUnreadCount(Long.parseLong(userId)));
    }

    @PostMapping("/mark-read")
    public Result<String> markAsRead(@RequestParam String type) {
        String userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "Unauthorized");
        }
        notificationService.markAsRead(Long.parseLong(userId), type);
        return Result.success("Marked as read");
    }

    @GetMapping
    public Result<Page<NotificationDO>> listNotifications(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        String userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "Unauthorized");
        }
        return Result.success(notificationService.listNotifications(Long.parseLong(userId), type, status, current, size));
    }
}




