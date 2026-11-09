package com.storyforge.domain.activity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 작업 한 건의 앞뒤 본문. 공개 활동 목록에는 넣지 않습니다. */
@Entity
@Table(name = "activity_review")
public class ActivityReview {

    @Id
    private Long eventId;

    @Column(columnDefinition = "text")
    private String beforeText;

    @Column(columnDefinition = "text")
    private String afterText;

    protected ActivityReview() {
    }

    public ActivityReview(Long eventId, String beforeText, String afterText) {
        this.eventId = eventId;
        this.beforeText = beforeText;
        this.afterText = afterText;
    }

    public Long getEventId() { return eventId; }
    public String getBeforeText() { return beforeText; }
    public String getAfterText() { return afterText; }
}
