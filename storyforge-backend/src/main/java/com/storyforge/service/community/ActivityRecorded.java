package com.storyforge.service.community;

/** 활동이 커밋된 뒤 업적을 다시 계산하라는 신호 */
public record ActivityRecorded(Long userId) {
}
