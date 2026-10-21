package com.storyforge.domain.chapter;

import com.storyforge.domain.project.Project;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "chapter")
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private int number;

    private String title;

    @Column(columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChapterStatus status;

    @Column(columnDefinition = "text")
    private String summary;

    /** 생성에 사용한 컨텍스트 (기획서 §9.1 투명성) */
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> generationContext;

    @Column(nullable = false)
    private Instant createdAt;

    protected Chapter() {
    }

    public Chapter(Project project, int number) {
        this.project = project;
        this.number = number;
        this.status = ChapterStatus.DRAFT;
        this.createdAt = Instant.now();
    }

    /** 생성(또는 재생성) 결과를 Draft로 저장합니다. */
    public void applyDraft(String title, String content, String summary, Map<String, Object> generationContext) {
        this.title = title;
        this.content = content;
        this.summary = summary;
        this.generationContext = generationContext;
        this.status = ChapterStatus.DRAFT;
    }

    /** 사용자 검토 중 직접 수정 */
    public void edit(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void confirm() {
        this.status = ChapterStatus.CONFIRMED;
    }

    public Long getId() { return id; }
    public Project getProject() { return project; }
    public int getNumber() { return number; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public ChapterStatus getStatus() { return status; }
    public String getSummary() { return summary; }
    public Map<String, Object> getGenerationContext() { return generationContext; }
    public Instant getCreatedAt() { return createdAt; }
}
