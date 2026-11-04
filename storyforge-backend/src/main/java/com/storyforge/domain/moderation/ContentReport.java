package com.storyforge.domain.moderation;

import com.storyforge.domain.user.AppUser;
import jakarta.persistence.*;

import java.time.Instant;

/** 신고. 관리자의 처리 결과와 처리한 사람도 여기에 남습니다. */
@Entity
@Table(name = "content_report")
public class ContentReport {

    public enum TargetType { WORK, POST, COMMENT, USER }

    public enum Reason { SPAM, ABUSE, SEXUAL, VIOLENCE, COPYRIGHT, PERSONAL_INFO, OTHER }

    public enum Status { OPEN, RESOLVED, DISMISSED }

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

    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @SuppressWarnings("unused") // 누가 처리했는지 남기는 기록. 화면에는 내보내지 않습니다.
    private Long resolvedBy;

    private String resolutionNote;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant resolvedAt;

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

    public void close(Status status, Long adminId, String note) {
        this.status = status;
        this.resolvedBy = adminId;
        this.resolutionNote = note;
        this.resolvedAt = Instant.now();
    }

    public Long getId() { return id; }
    public AppUser getReporter() { return reporter; }
    public TargetType getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public Reason getReason() { return reason; }
    public String getDetail() { return detail; }
    public Status getStatus() { return status; }
    public String getResolutionNote() { return resolutionNote; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
}
