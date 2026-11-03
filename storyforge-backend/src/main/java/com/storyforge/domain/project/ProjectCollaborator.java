package com.storyforge.domain.project;

import jakarta.persistence.*;

import java.time.Instant;

/** 프로젝트에 초대된 공동 창작자. 주인은 이 표에 넣지 않고 project.owner_id로 둡니다. */
@Entity
@Table(name = "project_collaborator")
public class ProjectCollaborator {

    public enum Status { PENDING, ACCEPTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long invitedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private Instant createdAt;

    protected ProjectCollaborator() {
    }

    public ProjectCollaborator(Long projectId, Long userId, Long invitedBy) {
        this.projectId = projectId;
        this.userId = userId;
        this.invitedBy = invitedBy;
        this.status = Status.PENDING;
        this.createdAt = Instant.now();
    }

    public void accept() {
        this.status = Status.ACCEPTED;
    }

    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public Long getUserId() { return userId; }
    public Long getInvitedBy() { return invitedBy; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
