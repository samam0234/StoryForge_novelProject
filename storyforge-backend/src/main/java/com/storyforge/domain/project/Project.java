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

    /** 인증 도입 전까지 비어 있습니다. */
    private Long ownerId;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Project() {
    }

    public Project(String title, String genre) {
        this.title = title;
        this.genre = genre;
        this.status = ProjectStatus.BIBLE_DRAFT;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /** 상태 변경은 PipelineStateMachine을 통해서만 호출합니다. */
    public void changeStatus(ProjectStatus status) {
        this.status = status;
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
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
