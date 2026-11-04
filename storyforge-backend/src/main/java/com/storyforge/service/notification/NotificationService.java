package com.storyforge.service.notification;

import com.storyforge.domain.community.CommunityDtos.PageResponse;
import com.storyforge.domain.moderation.UserBlockRepository;
import com.storyforge.domain.notification.Notification;
import com.storyforge.domain.notification.NotificationDtos.NotificationView;
import com.storyforge.domain.notification.NotificationDtos.UnreadCount;
import com.storyforge.domain.notification.NotificationRepository;
import com.storyforge.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 알림. 호출한 쪽 트랜잭션에 묶이므로 원래 작업이 실패하면 알림도 남지 않습니다.
 * 내 행동은 나에게 알리지 않고, 내가 차단한 사람의 행동도 알리지 않습니다. 본문은 넣지 않습니다.
 */
@Service
@Transactional(readOnly = true)
public class NotificationService {

    private static final int MAX_MESSAGE = 300;

    private final NotificationRepository notifications;
    private final UserBlockRepository blocks;

    public NotificationService(NotificationRepository notifications, UserBlockRepository blocks) {
        this.notifications = notifications;
        this.blocks = blocks;
    }

    /** 다른 사람의 행동으로 생긴 알림 */
    @Transactional
    public void notify(Long userId, Long actorId, Notification.Type type, String message, String link) {
        if (userId == null || userId.equals(actorId)
                || (actorId != null && blocks.existsByBlockerIdAndBlockedId(userId, actorId))) {
            return;
        }
        send(userId, type, message, link);
    }

    /** 시스템이 보내는 알림 (달성, 업적, 관리자 조치) */
    @Transactional
    public void send(Long userId, Notification.Type type, String message, String link) {
        String text = message.length() > MAX_MESSAGE ? message.substring(0, MAX_MESSAGE) : message;
        notifications.save(new Notification(userId, type, text, link));
    }

    public PageResponse<NotificationView> list(Long userId, int page, int size) {
        Page<Notification> result = notifications.findByUserIdOrderByCreatedAtDescIdDesc(userId,
                PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 50)));
        return new PageResponse<>(result.stream().map(n -> NotificationView.from(n)).toList(),
                result.getTotalElements(), result.getNumber(), result.getSize());
    }

    public UnreadCount unread(Long userId) {
        return new UnreadCount(notifications.countByUserIdAndReadAtIsNull(userId));
    }

    @Transactional
    public NotificationView read(Long id, Long userId) {
        Notification n = notifications.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("알림을 찾을 수 없습니다: " + id));
        n.read();
        return NotificationView.from(n);
    }

    @Transactional
    public void readAll(Long userId) {
        notifications.readAll(userId, Instant.now());
    }
}
