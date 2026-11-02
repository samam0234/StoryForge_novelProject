package com.storyforge.domain.activity;

import com.storyforge.domain.community.Post;
import com.storyforge.domain.community.Work;
import jakarta.persistence.*;

import java.time.Instant;

/** 작업 그래프와 활동 잔디의 원본. 요약에 본문을 넣지 않습니다 (게시하면 공개됨). */
@Entity
@Table(name = "activity_event")
public class ActivityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    private Long projectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_id")
    private Work work;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType.Actor actor;

    @Column(nullable = false)
    private String summary;

    @Column(nullable = false)
    private int changedChars;

    @Column(nullable = false)
    private Instant createdAt;

    protected ActivityEvent() {
    }

    public ActivityEvent(Long userId, Long projectId, Work work, Post post, ActivityType type, String summary, int changedChars) {
        this.userId = userId;
        this.projectId = projectId;
        this.work = work;
        this.post = post;
        this.type = type;
        this.actor = type.actor();
        this.summary = summary;
        this.changedChars = changedChars;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getProjectId() { return projectId; }
    public Work getWork() { return work; }
    public Post getPost() { return post; }
    public ActivityType getType() { return type; }
    public ActivityType.Actor getActor() { return actor; }
    public String getSummary() { return summary; }
    public int getChangedChars() { return changedChars; }
    public Instant getCreatedAt() { return createdAt; }
}
