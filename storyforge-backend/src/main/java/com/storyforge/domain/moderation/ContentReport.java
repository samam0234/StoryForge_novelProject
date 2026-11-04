package com.storyforge.domain.moderation;

import com.storyforge.domain.user.AppUser;
import jakarta.persistence.*;

import java.time.Instant;

/** 게시글·작품·댓글·사용자 신고. 같은 대상의 열린 신고는 신고자당 하나입니다. */
@Entity
@Table(name = "content_report")
public class ContentReport {

    public enum TargetType { POST, WORK, COMMENT, USER }

    public enum Reason { SPAM, ABUSE, ILLEGAL, COPYRIGHT, OTHER }

    public enum Status { OPEN, DISMISSED, HIDDEN }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private AppUser reporter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Reason reason;

    @Column(length = 1000)
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolver_id")
    private AppUser resolver;

    protected ContentReport() {
    }

    public ContentReport(AppUser reporter, TargetType targetType, Long targetId, Reason reason, String detail) {
        this.reporter = reporter;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        this.detail = detail;
        this.status = Status.OPEN;
        this.createdAt = Instant.now();
    }

    public void resolve(Status status, AppUser resolver) {
        this.status = status;
        this.resolver = resolver;
        this.resolvedAt = Instant.now();
    }

    public Long getId() { return id; }
    public AppUser getReporter() { return reporter; }
    public TargetType getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public Reason getReason() { return reason; }
    public String getDetail() { return detail; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
}
