package com.storyforge.service.community;

import com.storyforge.domain.community.CommunityDtos.Celebration;
import com.storyforge.domain.community.CommunityDtos.IssueBoard;
import com.storyforge.domain.community.CommunityDtos.WorkCard;
import com.storyforge.domain.community.IssueEvent;
import com.storyforge.domain.community.IssueEvent.Metric;
import com.storyforge.domain.community.IssueEventRepository;
import com.storyforge.domain.community.Work;
import com.storyforge.domain.community.WorkRepository;
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

    public IssueService(IssueEventRepository issues, WorkRepository works) {
        this.issues = issues;
        this.works = works;
    }

    /** 지표가 단위를 넘으면 축하를 남깁니다. 호출한 쪽 트랜잭션에 함께 묶입니다. */
    @Transactional
    public void reached(Long workId, Metric metric, long value) {
        for (int threshold : THRESHOLDS.get(metric)) {
            if (value >= threshold) {
                issues.reach(workId, metric.name(), threshold);
            }
        }
    }

    public IssueBoard board() {
        List<Long> ids = works.trendingIds(Instant.now().minus(Duration.ofDays(7)));
        Map<Long, Work> found = ids.isEmpty() ? Map.of()
                : works.findAllWithAuthor(ids).stream().collect(Collectors.toMap(w -> w.getId(), Function.identity()));
        List<WorkCard> trending = ids.stream().map(found::get)
                .filter(w -> w != null && w.getHiddenAt() == null).map(WorkCard::from).toList();
        List<Celebration> celebrations = issues.recent().stream()
                .filter(e -> e.getWork().getHiddenAt() == null)
                .map(e -> new Celebration(e.getId(), e.getMetric(), e.getThreshold(), message(e), WorkCard.from(e.getWork()), e.getCreatedAt()))
                .toList();
        List<WorkCard> fresh = works.findTop12ByVisibilityOrderByPublishedAtDesc(Work.Visibility.PUBLIC).stream()
                .filter(w -> w.getHiddenAt() == null)
                .map(WorkCard::from).toList();
        return new IssueBoard(trending, celebrations, fresh);
    }

    private static String message(IssueEvent e) {
        String n = NumberFormat.getIntegerInstance(Locale.KOREA).format(e.getThreshold());
        return switch (e.getMetric()) {
            case DAILY_LIKES -> "하루 만에 추천 " + n + "개를 받았습니다";
            case TOTAL_LIKES -> "누적 추천 " + n + "개를 달성했습니다";
            case TOTAL_CLONES -> "누적 Clone " + n + "회를 달성했습니다";
            case TOTAL_SHARES -> "누적 공유 " + n + "회를 달성했습니다";
            case TOTAL_VIEWS -> "누적 조회 " + n + "회를 달성했습니다";
        };
    }
}
