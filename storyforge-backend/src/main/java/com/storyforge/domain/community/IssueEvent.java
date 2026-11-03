package com.storyforge.domain.community;

import jakarta.persistence.*;

import java.time.Instant;

/** 이슈 게시판의 달성 축하 (예: 하루 만에 추천 100개) */
@Entity
@Table(name = "issue_event")
public class IssueEvent {

    public enum Metric { DAILY_LIKES, TOTAL_LIKES, TOTAL_CLONES, TOTAL_SHARES, TOTAL_VIEWS }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_id", nullable = false)
    private Work work;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Metric metric;

    @Column(nullable = false)
    private int threshold;

    @Column(nullable = false)
    private Instant createdAt;

    protected IssueEvent() {
    }

    public Long getId() { return id; }
    public Work getWork() { return work; }
    public Metric getMetric() { return metric; }
    public int getThreshold() { return threshold; }
    public Instant getCreatedAt() { return createdAt; }
}
