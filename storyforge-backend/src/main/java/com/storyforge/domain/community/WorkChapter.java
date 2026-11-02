package com.storyforge.domain.community;

import jakarta.persistence.*;

@Entity
@Table(name = "work_chapter")
public class WorkChapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_id", nullable = false)
    private Work work;

    @Column(nullable = false)
    private int number;

    private String title;

    @Column(columnDefinition = "text")
    private String content;

    /** Clone할 때 다음 챕터 생성 컨텍스트로 넘길 요약 */
    @Column(columnDefinition = "text")
    private String summary;

    protected WorkChapter() {
    }

    public WorkChapter(Work work, int number) {
        this.work = work;
        this.number = number;
    }

    public void apply(String title, String content, String summary) {
        this.title = title;
        this.content = content;
        this.summary = summary;
    }

    public void edit(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public Long getId() { return id; }
    public int getNumber() { return number; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getSummary() { return summary; }
}
