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

    @Version
    @Column(nullable = false)
    private long version;

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

    /** 본문이 요약·Continuity 결과와 어긋날 수 있어 승인 전에 다시 검증해야 하는 상태 */
    @Column(nullable = false)
    private boolean needsReview;

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
        this.needsReview = false;
    }

    public void markNeedsReview() {
        this.needsReview = true;
    }

    /** 재검증 결과 반영. 다음 챕터 컨텍스트에는 이 요약이 들어갑니다. */
    public void applyReview(String summary) {
        this.summary = summary;
        this.needsReview = false;
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
    public long getVersion() { return version; }
    public Project getProject() { return project; }
    public int getNumber() { return number; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public ChapterStatus getStatus() { return status; }
    public String getSummary() { return summary; }
    public boolean isNeedsReview() { return needsReview; }
    public Map<String, Object> getGenerationContext() { return generationContext; }
    public Instant getCreatedAt() { return createdAt; }
}
