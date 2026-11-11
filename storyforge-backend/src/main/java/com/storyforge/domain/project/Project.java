package com.storyforge.domain.project;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "project")
public class Project {

    /** 장편·단편 구분. 게시 종류와 Outline 챕터 수 기본값에 씁니다. */
    public enum Form { LONG, SHORT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String genre;

    @Column(columnDefinition = "text")
    private String creativeBrief;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status;

    /** 소유자. 인증 도입 전 데이터만 비어 있습니다. */
    private Long ownerId;

    /** 표지 이미지 주소. http(s)만 저장합니다. */
    private String coverUrl;

    /** 생성에 쓸 모델. 비어 있으면 AI 서비스 기본값 (기획서 §9.6-5) */
    private String model;

    /** Bible 생성 때 반영할 기본 톤 */
    @Column(columnDefinition = "text")
    private String defaultTone;

    @Column(nullable = false)
    private String exportFormat = "md";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Form form = Form.LONG;

    /** Clone 출처. 원본 작품을 지워도 제목과 리비전은 남습니다. */
    private Long clonedFromWorkId;

    private String clonedFromTitle;

    private Integer clonedFromRevision;

    private String cloneMode;

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

    public Project(String title, String genre, Long ownerId, Form form) {
        this(title, genre, ownerId);
        this.form = form == null ? Form.LONG : form;
    }

    /**
     * Clone 프로젝트 생성. 범위에 맞는 단계에서 시작합니다 (전이가 아니라 시작 상태이며, 이후 전이는 상태 머신을 따릅니다).
     * docs/Architecture/pipeline-state-machine.md
     */
    public static Project cloned(String title, String genre, Long ownerId, ProjectStatus start, Form form,
                                 Long workId, String workTitle, int revision, String mode) {
        Project p = new Project(title, genre, ownerId, form);
        p.status = start;
        p.clonedFromWorkId = workId;
        p.clonedFromTitle = workTitle;
        p.clonedFromRevision = revision;
        p.cloneMode = mode;
        return p;
    }

    /** 원본의 이 리비전까지 확인했다고 표시합니다. */
    public void acknowledgeUpstream(int revision) {
        this.clonedFromRevision = revision;
        touch();
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
    public String getCreativeBrief() { return creativeBrief; }
    public void setCreativeBrief(String creativeBrief) { this.creativeBrief = creativeBrief; touch(); }
    public ProjectStatus getStatus() { return status; }
    public Long getOwnerId() { return ownerId; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public String getModel() { return model; }
    public String getDefaultTone() { return defaultTone; }
    public String getExportFormat() { return exportFormat; }
    public Form getForm() { return form; }
    public Long getClonedFromWorkId() { return clonedFromWorkId; }
    public String getClonedFromTitle() { return clonedFromTitle; }
    public Integer getClonedFromRevision() { return clonedFromRevision; }
    public String getCloneMode() { return cloneMode; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
