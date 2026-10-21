package com.storyforge.domain.project;

/**
 * 프로젝트 단위 파이프라인 상태 (기획서 v1.2 §7.2). 이름과 순서를 임의로 바꾸지 않습니다.
 */
public enum ProjectStatus {
    BIBLE_DRAFT,
    BIBLE_CONFIRMED,
    OUTLINE_DRAFT,
    OUTLINE_CONFIRMED,
    CHAPTER_GENERATING,
    CHAPTER_REVIEW,
    COMPLETED
}
