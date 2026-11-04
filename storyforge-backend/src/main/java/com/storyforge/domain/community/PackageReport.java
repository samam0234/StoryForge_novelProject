package com.storyforge.domain.community;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

/** 패키지 전체 Continuity 검사 결과. 다른 작가의 비공개 설정이 드러날 수 있어 패키지 주인만 봅니다. */
@Entity
@Table(name = "package_report")
public class PackageReport {

    public record Issue(String type, String description, String severity, List<String> workTitles) {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long packageWorkId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<Issue> issues;

    @Column(nullable = false)
    private int score;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> skippedTitles;

    @Column(nullable = false)
    private Instant createdAt;

    protected PackageReport() {
    }

    public PackageReport(Long packageWorkId, List<Issue> issues, int score, List<String> skippedTitles) {
        this.packageWorkId = packageWorkId;
        this.issues = issues;
        this.score = score;
        this.skippedTitles = skippedTitles;
        this.createdAt = Instant.now();
    }

    public List<Issue> getIssues() { return issues; }
    public int getScore() { return score; }
    public List<String> getSkippedTitles() { return skippedTitles; }
    public Instant getCreatedAt() { return createdAt; }
}
