package com.storyforge.domain.activity;

import java.time.Instant;
import java.util.List;

public final class ActivityDtos {

    private ActivityDtos() {
    }

    public record ActivityEntry(Long id, ActivityType type, ActivityType.Actor actor, String summary, int changedChars,
                                Instant createdAt, String day, Long workId, String workTitle, Long postId,
                                String postTitle, Long projectId, String projectTitle) {
    }

    public record ActorShare(long events, long chars) {
    }

    /** 작업 그래프의 하루치. 그날 수동·에이전트 작업 횟수와 변경 글자 수 */
    public record GraphDay(String date, int manual, int agent, long manualChars, long agentChars) {
    }

    /** 작업 그래프 (수동·에이전트 비율, 날짜별 막대, 기록 표) */
    public record WorkGraph(ActorShare manual, ActorShare agent, List<GraphDay> days, List<ActivityEntry> events) {
    }

    public record ContributionDay(String date, int count, int level) {
    }

    /** 활동 잔디 */
    public record Contributions(String from, String to, long total, int currentStreak, int longestStreak,
                                List<ContributionDay> days) {
    }

    public record AchievementView(String code, String name, String description, String icon, String tier, long progress,
                                  long target, Instant earnedAt) {
    }
}
