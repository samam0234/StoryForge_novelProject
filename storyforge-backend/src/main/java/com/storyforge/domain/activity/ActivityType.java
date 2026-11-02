package com.storyforge.domain.activity;

/**
 * 활동 종류. actor로 수동과 에이전트를 나누고, authoring이면 작업 그래프에 들어갑니다.
 * Clone·공유·댓글 같은 커뮤니티 활동은 활동 잔디에만 셉니다.
 */
public enum ActivityType {
    PROJECT_CREATED(Actor.MANUAL, true),
    BIBLE_GENERATED(Actor.AGENT, true),
    BIBLE_EDITED(Actor.MANUAL, true),
    BIBLE_CONFIRMED(Actor.MANUAL, true),
    OUTLINE_GENERATED(Actor.AGENT, true),
    OUTLINE_EDITED(Actor.MANUAL, true),
    OUTLINE_CONFIRMED(Actor.MANUAL, true),
    CHAPTER_GENERATED(Actor.AGENT, true),
    CHAPTER_EDITED(Actor.MANUAL, true),
    CHAPTER_CONFIRMED(Actor.MANUAL, true),
    WORK_PUBLISHED(Actor.MANUAL, true),
    WORK_UPDATED(Actor.MANUAL, true),
    WORK_SYNCED(Actor.MANUAL, true),
    WORK_CHAPTER_EDITED(Actor.MANUAL, true),
    PACKAGE_CREATED(Actor.MANUAL, false),
    PACKAGE_UPDATED(Actor.MANUAL, false),
    PROJECT_CLONED(Actor.MANUAL, false),
    WORK_SHARED(Actor.MANUAL, false),
    COMMENT_WRITTEN(Actor.MANUAL, false),
    POST_WRITTEN(Actor.MANUAL, false);

    public enum Actor { MANUAL, AGENT }

    private final Actor actor;
    private final boolean authoring;

    ActivityType(Actor actor, boolean authoring) {
        this.actor = actor;
        this.authoring = authoring;
    }

    public Actor actor() {
        return actor;
    }

    public boolean authoring() {
        return authoring;
    }
}
