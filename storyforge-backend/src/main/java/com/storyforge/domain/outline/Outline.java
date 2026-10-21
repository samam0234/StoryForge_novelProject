package com.storyforge.domain.outline;

import com.storyforge.domain.project.Project;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "outline")
public class Outline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, unique = true)
    private Project project;

    /** 기획서 §10: chapters[] (order, title, beats, characters, goals, continuityPoints) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<OutlineChapter> chapters = new ArrayList<>();

    @Column(nullable = false)
    private Instant updatedAt;

    protected Outline() {
    }

    public Outline(Project project) {
        this.project = project;
        this.updatedAt = Instant.now();
    }

    public void replaceChapters(List<OutlineChapter> chapters) {
        this.chapters = new ArrayList<>(chapters);
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Project getProject() { return project; }
    public List<OutlineChapter> getChapters() { return chapters; }
    public Instant getUpdatedAt() { return updatedAt; }
}
