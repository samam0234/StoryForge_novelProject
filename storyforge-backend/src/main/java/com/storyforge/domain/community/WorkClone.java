package com.storyforge.domain.community;

import com.storyforge.domain.user.AppUser;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * Clone 기록. 공유(읽을 링크 전달)와 달리 원본의 창작 자료를 내 프로젝트로 가져온 파생 기록입니다.
 */
@Entity
@Table(name = "work_clone")
public class WorkClone {

    /** WORLD: Bible만 / PLOT: Bible + Outline / FULL: Bible + Outline + 승인된 원고 */
    public enum Mode { WORLD, PLOT, FULL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long workId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    private Long projectId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Mode mode;

    @Column(nullable = false)
    private int revision;

    @Column(nullable = false)
    private Instant createdAt;

    protected WorkClone() {
    }

    public WorkClone(Long workId, AppUser user, Long projectId, Mode mode, int revision) {
        this.workId = workId;
        this.user = user;
        this.projectId = projectId;
        this.mode = mode;
        this.revision = revision;
        this.createdAt = Instant.now();
    }

    public AppUser getUser() { return user; }
    public Long getProjectId() { return projectId; }
    public Mode getMode() { return mode; }
    public int getRevision() { return revision; }
    public Instant getCreatedAt() { return createdAt; }
}
