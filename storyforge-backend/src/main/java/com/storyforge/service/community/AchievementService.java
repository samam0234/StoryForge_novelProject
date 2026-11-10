package com.storyforge.service.community;

import com.storyforge.domain.activity.ActivityDtos.AchievementView;
import com.storyforge.domain.activity.ActivityEventRepository;
import com.storyforge.domain.activity.ActivityType;
import com.storyforge.domain.community.CommentRepository;
import com.storyforge.domain.community.PostRepository;
import com.storyforge.domain.community.Work;
import com.storyforge.domain.community.WorkRepository;
import com.storyforge.domain.notification.Notification;
import com.storyforge.domain.project.ProjectRepository;
import com.storyforge.domain.project.ProjectStatus;
import com.storyforge.service.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToLongFunction;

/**
 * 업적(배지). 활동이 커밋된 뒤 계산하므로 업적 계산이 실패해도 파이프라인 작업은 되돌아가지 않습니다.
 */
@Service
public class AchievementService {

    private static final Logger log = LoggerFactory.getLogger(AchievementService.class);

    private record Stats(long projects, long agentChapters, long manualEdits, long manualChars, long completed,
                         long published, long longs, long shorts, long packages, long likes, long cloned, long clonesMade,
                         long comments, long posts, long shares, int longestStreak, int activeDays) {
    }

    private record Definition(String code, String name, String description, String icon, String tier, long target,
                              ToLongFunction<Stats> metric) {
    }

    private static final List<Definition> DEFINITIONS = List.of(
            new Definition("FIRST_PROJECT", "첫 불씨", "첫 프로젝트를 시작했습니다", "local_fire_department", "BRONZE", 1, s -> s.projects()),
            new Definition("FIRST_AGENT_CHAPTER", "첫 합작", "에이전트와 함께 첫 챕터를 만들었습니다", "smart_toy", "BRONZE", 1, s -> s.agentChapters()),
            new Definition("FIRST_MANUAL_EDIT", "직접 벼린 문장", "AI 원고를 직접 고쳤습니다", "edit_note", "BRONZE", 1, s -> s.manualEdits()),
            new Definition("MANUAL_10K", "장인의 손", "직접 고친 글자가 1만 자를 넘었습니다", "draw", "GOLD", 10_000, s -> s.manualChars()),
            new Definition("COMPLETED_1", "완주", "프로젝트 하나를 끝까지 완성했습니다", "flag", "SILVER", 1, s -> s.completed()),
            new Definition("COMPLETED_5", "다섯 번의 완결", "프로젝트 5개를 완성했습니다", "military_tech", "GOLD", 5, s -> s.completed()),
            new Definition("FIRST_PUBLISH", "첫 출간", "작품을 처음 게시했습니다", "publish", "BRONZE", 1, s -> s.published()),
            new Definition("FIRST_LONG", "롱품 작가", "장편을 게시했습니다", "menu_book", "SILVER", 1, s -> s.longs()),
            new Definition("FIRST_SHORT", "숏품 작가", "단편을 게시했습니다", "article", "BRONZE", 1, s -> s.shorts()),
            new Definition("FIRST_PACKAGE", "세계를 잇다", "패키지포지로 작품을 묶었습니다", "hub", "SILVER", 1, s -> s.packages()),
            new Definition("LIKES_10", "첫 응원", "추천 10개를 받았습니다", "favorite", "BRONZE", 10, s -> s.likes()),
            new Definition("LIKES_100", "인기 작가", "추천 100개를 받았습니다", "favorite", "SILVER", 100, s -> s.likes()),
            new Definition("LIKES_1000", "베스트셀러", "추천 1,000개를 받았습니다", "workspace_premium", "GOLD", 1_000, s -> s.likes()),
            new Definition("LIKES_10000", "전설의 작가", "추천 10,000개를 받았습니다", "diamond", "PLATINUM", 10_000, s -> s.likes()),
            new Definition("CLONED_1", "세계의 문을 열다", "다른 작가가 내 작품을 Clone했습니다", "call_split", "SILVER", 1, s -> s.cloned()),
            new Definition("CLONED_10", "세계관 창시자", "내 작품이 10번 Clone되었습니다", "account_tree", "GOLD", 10, s -> s.cloned()),
            new Definition("CLONE_MADE_1", "이어 쓰는 사람", "다른 작품을 Clone해 시작했습니다", "fork_right", "BRONZE", 1, s -> s.clonesMade()),
            new Definition("COMMENT_10", "수다쟁이", "댓글을 10개 남겼습니다", "forum", "BRONZE", 10, s -> s.comments()),
            new Definition("POST_1", "라운지 입장", "창작 라운지에 글을 썼습니다", "chat", "BRONZE", 1, s -> s.posts()),
            new Definition("SHARE_10", "입소문", "작품을 10번 공유했습니다", "campaign", "SILVER", 10, s -> s.shares()),
            new Definition("STREAK_7", "일주일 연속", "7일 연속 활동했습니다", "calendar_month", "SILVER", 7, s -> s.longestStreak()),
            new Definition("STREAK_30", "한 달 연속", "30일 연속 활동했습니다", "event_available", "GOLD", 30, s -> s.longestStreak()),
            new Definition("ACTIVE_100", "백일 작가", "활동한 날이 100일을 넘었습니다", "emoji_events", "GOLD", 100, s -> s.activeDays()));

    private final ActivityEventRepository events;
    private final ActivityService activity;
    private final WorkRepository works;
    private final ProjectRepository projects;
    private final CommentRepository comments;
    private final PostRepository posts;
    private final JdbcTemplate jdbc;
    private final NotificationService notifications;

    public AchievementService(ActivityEventRepository events, ActivityService activity, WorkRepository works,
                              ProjectRepository projects, CommentRepository comments, PostRepository posts, JdbcTemplate jdbc,
                              NotificationService notifications) {
        this.notifications = notifications;
        this.events = events;
        this.activity = activity;
        this.works = works;
        this.projects = projects;
        this.comments = comments;
        this.posts = posts;
        this.jdbc = jdbc;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onActivity(ActivityRecorded event) {
        try {
            Stats stats = stats(event.userId());
            for (Definition d : DEFINITIONS) {
                if (d.metric().applyAsLong(stats) >= d.target()) {
                    int inserted = jdbc.update("INSERT INTO user_achievement (user_id, code, earned_at) VALUES (?, ?, now()) ON CONFLICT DO NOTHING",
                            event.userId(), d.code());
                    if (inserted > 0) {
                        notifications.send(event.userId(), Notification.Type.ACHIEVEMENT,
                                "업적을 달성했습니다: " + d.name() + " — " + d.description(), "/users/" + event.userId());
                    }
                }
            }
        } catch (RuntimeException e) {
            log.warn("achievement evaluation failed: user={}", event.userId(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<AchievementView> list(Long userId) {
        Map<String, Instant> earned = new HashMap<>();
        jdbc.query("SELECT code, earned_at FROM user_achievement WHERE user_id = ?",
                rs -> {
                    Timestamp at = rs.getTimestamp("earned_at");
                    earned.put(rs.getString("code"), at.toInstant());
                }, userId);
        Stats stats = stats(userId);
        return DEFINITIONS.stream()
                .map(d -> new AchievementView(d.code(), d.name(), d.description(), d.icon(), d.tier(),
                        Math.min(d.metric().applyAsLong(stats), d.target()), d.target(), earned.get(d.code())))
                .toList();
    }

    @Transactional(readOnly = true)
    public long earnedCount(Long userId) {
        return Objects.requireNonNull(jdbc.queryForObject("SELECT count(*) FROM user_achievement WHERE user_id = ?", Long.class, userId));
    }

    private Stats stats(Long userId) {
        ActivityService.Streaks streaks = activity.streaks(userId);
        long longs = works.countByAuthorIdAndKind(userId, Work.Kind.LONG);
        long shorts = works.countByAuthorIdAndKind(userId, Work.Kind.SHORT);
        long packages = works.countByAuthorIdAndKind(userId, Work.Kind.PACKAGE);
        return new Stats(
                events.countByUserIdAndType(userId, ActivityType.PROJECT_CREATED),
                events.countByUserIdAndType(userId, ActivityType.CHAPTER_GENERATED),
                events.countByUserIdAndType(userId, ActivityType.CHAPTER_EDITED)
                        + events.countByUserIdAndType(userId, ActivityType.WORK_CHAPTER_EDITED),
                events.sumChars(userId, ActivityType.Actor.MANUAL),
                projects.countByOwnerIdAndStatus(userId, ProjectStatus.COMPLETED),
                longs + shorts + packages, longs, shorts, packages,
                works.sumLikes(userId), works.sumClones(userId),
                events.countByUserIdAndType(userId, ActivityType.PROJECT_CLONED),
                comments.countByAuthorId(userId), posts.countByAuthorId(userId),
                events.countByUserIdAndType(userId, ActivityType.WORK_SHARED),
                streaks.longest(), streaks.activeDays());
    }
}
