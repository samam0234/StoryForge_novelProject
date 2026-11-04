package com.storyforge.domain.community;

import com.storyforge.domain.bible.BibleDtos.BibleSnapshot;
import com.storyforge.domain.outline.OutlineChapter;
import com.storyforge.domain.user.AppUser;
import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

/**
 * 게시된 작품. 프로젝트와 독립된 스냅샷이라 프로젝트를 지워도 남습니다.
 * 카운터는 리포지토리의 증감 쿼리로만 바꾸므로, 다른 필드 저장이 덮어쓰지 않게 변경된 컬럼만 UPDATE합니다.
 */
@Entity
@Table(name = "work")
@DynamicUpdate
public class Work {

    /** 롱품, 숏품, 패키지포지 */
    public enum Kind { LONG, SHORT, PACKAGE }

    public enum Visibility { PUBLIC, UNLISTED, PRIVATE }

    /** OPEN: 세계관·플롯·전체 / WORLD_ONLY: 세계관만 / CLOSED: 불가 */
    public enum ClonePolicy { OPEN, WORLD_ONLY, CLOSED }

    public enum PackageType { UNIVERSE, OMNIBUS, ANTHOLOGY, SAGA, CROSSOVER, SPINOFF, CUSTOM }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private AppUser author;

    private Long projectId;

    private Long parentWorkId;

    private String parentTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Kind kind;

    @Enumerated(EnumType.STRING)
    private PackageType packageType;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String synopsis;

    private String genre;

    /** 쉼표로 구분한 태그 */
    private String tags;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClonePolicy clonePolicy;

    @Column(nullable = false)
    private boolean downloadable;

    /** 패키지 공용 세계관 노트 */
    @Column(columnDefinition = "text")
    private String worldNote;

    @JdbcTypeCode(SqlTypes.JSON)
    private BibleSnapshot bibleSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<OutlineChapter> outlineSnapshot;

    @Column(nullable = false)
    private int revision;

    @Column(nullable = false)
    private int chapterCount;

    @Column(nullable = false)
    private int charCount;

    @Column(nullable = false)
    private int likeCount;

    @Column(nullable = false)
    private int commentCount;

    @Column(nullable = false)
    private int cloneCount;

    @Column(nullable = false)
    private int shareCount;

    @Column(nullable = false)
    private long viewCount;

    /** 관리자가 숨긴 시각. 비어 있지 않으면 작가와 관리자만 봅니다. */
    private Instant hiddenAt;

    @Column(nullable = false)
    private Instant publishedAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Work() {
    }

    public Work(AppUser author, Kind kind, String title) {
        this.author = author;
        this.kind = kind;
        this.title = title;
        this.visibility = Visibility.PUBLIC;
        this.clonePolicy = ClonePolicy.OPEN;
        this.downloadable = true;
        this.revision = 1;
        this.publishedAt = Instant.now();
        this.updatedAt = this.publishedAt;
    }

    public void linkSource(Long projectId, Long parentWorkId, String parentTitle) {
        this.projectId = projectId;
        this.parentWorkId = parentWorkId;
        this.parentTitle = parentTitle;
    }

    public void updateMeta(String title, String synopsis, String genre, String tags, Visibility visibility,
                           ClonePolicy clonePolicy, boolean downloadable) {
        this.title = title;
        this.synopsis = synopsis;
        this.genre = genre;
        this.tags = tags;
        this.visibility = visibility;
        this.clonePolicy = clonePolicy;
        this.downloadable = downloadable;
        this.updatedAt = Instant.now();
    }

    public void updatePackage(PackageType packageType, String worldNote) {
        this.packageType = packageType;
        this.worldNote = worldNote;
        this.updatedAt = Instant.now();
    }

    public void applySnapshot(BibleSnapshot bible, List<OutlineChapter> outline) {
        this.bibleSnapshot = bible;
        this.outlineSnapshot = outline;
    }

    public void applyCounts(int chapterCount, int charCount) {
        this.chapterCount = chapterCount;
        this.charCount = charCount;
        this.updatedAt = Instant.now();
    }

    /** 본문이 바뀔 때마다 하나씩 올립니다. */
    public int nextRevision() {
        this.revision++;
        this.updatedAt = Instant.now();
        return this.revision;
    }

    public void setHidden(boolean hidden) {
        this.hiddenAt = hidden ? Instant.now() : null;
    }

    public boolean isHidden() {
        return hiddenAt != null;
    }

    /** 게시판과 프로필에 보여도 되는 상태 */
    public boolean isListed() {
        return visibility == Visibility.PUBLIC && hiddenAt == null;
    }

    public Long getId() { return id; }
    public AppUser getAuthor() { return author; }
    public Long getProjectId() { return projectId; }
    public Long getParentWorkId() { return parentWorkId; }
    public String getParentTitle() { return parentTitle; }
    public Kind getKind() { return kind; }
    public PackageType getPackageType() { return packageType; }
    public String getTitle() { return title; }
    public String getSynopsis() { return synopsis; }
    public String getGenre() { return genre; }
    public String getTags() { return tags; }
    public Visibility getVisibility() { return visibility; }
    public ClonePolicy getClonePolicy() { return clonePolicy; }
    public boolean isDownloadable() { return downloadable; }
    public String getWorldNote() { return worldNote; }
    public BibleSnapshot getBibleSnapshot() { return bibleSnapshot; }
    public List<OutlineChapter> getOutlineSnapshot() { return outlineSnapshot; }
    public int getRevision() { return revision; }
    public int getChapterCount() { return chapterCount; }
    public int getCharCount() { return charCount; }
    public int getLikeCount() { return likeCount; }
    public int getCommentCount() { return commentCount; }
    public int getCloneCount() { return cloneCount; }
    public int getShareCount() { return shareCount; }
    public long getViewCount() { return viewCount; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
