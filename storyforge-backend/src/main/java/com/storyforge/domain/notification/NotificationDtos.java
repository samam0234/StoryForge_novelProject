package com.storyforge.domain.notification;

import java.time.Instant;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record NotificationView(Long id, Notification.Type type, String message, String link, boolean read,
                                   Instant createdAt) {

        public static NotificationView from(Notification n) {
            return new NotificationView(n.getId(), n.getType(), n.getMessage(), n.getLink(), n.getReadAt() != null,
                    n.getCreatedAt());
        }
    }

    public record UnreadCount(long count) {
    }
}
