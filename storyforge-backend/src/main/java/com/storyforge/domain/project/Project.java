package com.storyforge.domain.project;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "project")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String genre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status;

    /** 소유자. 인증 도입 전 데이터만 비어 있습니다. */
    private Long ownerId;

    /** 생성에 쓸 모델. 비어 있으면 AI 서비스 기본값 (기획서 §9.6-5) */
    private String model;

    /** Bible 생성 때 반영할 기본 톤 */
    @Column(columnDefinition = "text")
    private String defaultTone;

    @Column(nullable = false)
    private String exportFormat = "md";

    /** 동시 요청 방지 (낙관적 락) */
    @Version
    private long version;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Project() {
    }

    public Project(String title, String genre, Long ownerId) {
        this.title = title;
        this.genre = genre;
        this.ownerId = ownerId;
        this.status = ProjectStatus.BIBLE_DRAFT;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /** 상태 변경은 PipelineStateMachine을 통해서만 호출합니다. */
    public void changeStatus(ProjectStatus status) {
        this.status = status;
        touch();
    }

    public void updateSettings(String title, String genre, String model, String defaultTone, String exportFormat) {
        this.title = title;
        this.genre = genre;
        this.model = model;
        this.defaultTone = defaultTone;
        this.exportFormat = exportFormat;
        touch();
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getGenre() { return genre; }
    public ProjectStatus getStatus() { return status; }
    public Long getOwnerId() { return ownerId; }
    public String getModel() { return model; }
    public String getDefaultTone() { return defaultTone; }
    public String getExportFormat() { return exportFormat; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
