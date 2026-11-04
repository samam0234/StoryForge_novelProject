package com.storyforge.domain.notification;

import jakarta.persistence.*;

import java.time.Instant;

/** 알림. 링크는 앱 안 경로만 저장합니다. */
@Entity
@Table(name = "notification")
public class Notification {

    public enum Type {
        COMMENT, REPLY, CLONE, SHARE, MILESTONE, ACHIEVEMENT,
        PACKAGE_REQUEST, PACKAGE_DECISION, COLLAB_INVITE, COLLAB_ACCEPT, MODERATION
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(nullable = false)
    private String message;

    private String link;

    private Instant readAt;

    @Column(nullable = false)
    private Instant createdAt;

    protected Notification() {
    }

    public Notification(Long userId, Type type, String message, String link) {
        this.userId = userId;
        this.type = type;
        this.message = message;
        this.link = link;
        this.createdAt = Instant.now();
    }

    public void read() {
        if (readAt == null) {
            readAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Type getType() { return type; }
    public String getMessage() { return message; }
    public String getLink() { return link; }
    public Instant getReadAt() { return readAt; }
    public Instant getCreatedAt() { return createdAt; }
}
