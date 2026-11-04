package com.storyforge.service.community;

import com.storyforge.domain.community.CommunityDtos.Celebration;
import com.storyforge.domain.community.CommunityDtos.IssueBoard;
import com.storyforge.domain.community.CommunityDtos.WorkCard;
import com.storyforge.domain.community.IssueEvent.Metric;
import com.storyforge.domain.community.IssueEventRepository;
import com.storyforge.domain.community.Work;
import com.storyforge.domain.community.WorkRepository;
import com.storyforge.domain.notification.Notification;
import com.storyforge.service.notification.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 이슈 게시판: 최근 인기 작품과 달성 축하 (docs/plan/community.md) */
@Service
@Transactional(readOnly = true)
public class IssueService {

    private static final Map<Metric, int[]> THRESHOLDS = Map.of(
            Metric.DAILY_LIKES, new int[]{100, 1_000, 10_000},
            Metric.TOTAL_LIKES, new int[]{100, 1_000, 10_000},
            Metric.TOTAL_CLONES, new int[]{10, 100, 1_000},
            Metric.TOTAL_SHARES, new int[]{10, 100, 1_000},
            Metric.TOTAL_VIEWS, new int[]{1_000, 10_000, 100_000});

    private final IssueEventRepository issues;
    private final WorkRepository works;
    private final NotificationService notifications;

    public IssueService(IssueEventRepository issues, WorkRepository works, NotificationService notifications) {
        this.notifications = notifications;
        this.issues = issues;
        this.works = works;
    }

    /** 지표가 단위를 넘으면 축하를 남깁니다. 호출한 쪽 트랜잭션에 함께 묶입니다. */
    @Transactional
    public void reached(Long workId, Metric metric, long value) {
        for (int threshold : THRESHOLDS.get(metric)) {
            // 처음 넘었을 때만 작가에게 알립니다. 같은 단위는 다시 기록되지 않습니다.
            if (value >= threshold && issues.reach(workId, metric.name(), threshold) > 0) {
                works.findWithAuthor(workId).ifPresent(w -> notifications.send(w.getAuthor().getId(), Notification.Type.MILESTONE,
                        "축하합니다! 「" + w.getTitle() + "」이(가) " + message(metric, threshold), "/works/" + workId));
            }
        }
    }

    public IssueBoard board() {
        List<Long> ids = works.trendingIds(Instant.now().minus(Duration.ofDays(7)));
        Map<Long, Work> found = ids.isEmpty() ? Map.of()
                : works.findAllWithAuthor(ids).stream().collect(Collectors.toMap(w -> w.getId(), Function.identity()));
        List<WorkCard> trending = ids.stream().map(found::get).filter(w -> w != null).map(WorkCard::from).toList();
        List<Celebration> celebrations = issues.recent().stream()
                .map(e -> new Celebration(e.getId(), e.getMetric(), e.getThreshold(), message(e.getMetric(), e.getThreshold()), WorkCard.from(e.getWork()), e.getCreatedAt()))
                .toList();
        List<WorkCard> fresh = works.findTop12ByVisibilityAndHiddenAtIsNullOrderByPublishedAtDesc(Work.Visibility.PUBLIC).stream()
                .map(WorkCard::from).toList();
        return new IssueBoard(trending, celebrations, fresh);
    }

    private static String message(Metric metric, int threshold) {
        String n = NumberFormat.getIntegerInstance(Locale.KOREA).format(threshold);
        return switch (metric) {
            case DAILY_LIKES -> "하루 만에 추천 " + n + "개를 받았습니다";
            case TOTAL_LIKES -> "누적 추천 " + n + "개를 달성했습니다";
            case TOTAL_CLONES -> "누적 Clone " + n + "회를 달성했습니다";
            case TOTAL_SHARES -> "누적 공유 " + n + "회를 달성했습니다";
            case TOTAL_VIEWS -> "누적 조회 " + n + "회를 달성했습니다";
        };
    }
}
