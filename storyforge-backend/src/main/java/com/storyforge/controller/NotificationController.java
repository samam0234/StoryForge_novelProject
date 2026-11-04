package com.storyforge.controller;

import com.storyforge.domain.community.CommunityDtos.PageResponse;
import com.storyforge.domain.notification.NotificationDtos.NotificationView;
import com.storyforge.domain.notification.NotificationDtos.UnreadCount;
import com.storyforge.service.auth.CurrentUser;
import com.storyforge.service.notification.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** 내 알림. 다른 사람의 알림은 404입니다. */
@RestController
@RequestMapping("/api/v1/me/notifications")
public class NotificationController {

    private final NotificationService notifications;
    private final CurrentUser currentUser;

    public NotificationController(NotificationService notifications, CurrentUser currentUser) {
        this.notifications = notifications;
        this.currentUser = currentUser;
    }

    @GetMapping
    public PageResponse<NotificationView> list(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return notifications.list(currentUser.id(), page, size);
    }

    @GetMapping("/unread-count")
    public UnreadCount unread() {
        return notifications.unread(currentUser.id());
    }

    @PutMapping("/{notificationId}/read")
    public NotificationView read(@PathVariable Long notificationId) {
        return notifications.read(notificationId, currentUser.id());
    }

    @PutMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void readAll() {
        notifications.readAll(currentUser.id());
    }
}
