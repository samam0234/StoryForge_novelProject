package com.storyforge.service.community;

import com.storyforge.domain.activity.ActivityDtos.*;
import com.storyforge.domain.activity.ActivityEvent;
import com.storyforge.domain.activity.ActivityEventRepository;
import com.storyforge.domain.activity.ActivityReview;
import com.storyforge.domain.activity.ActivityReviewRepository;
import com.storyforge.domain.activity.ActivityType;
import com.storyforge.domain.community.CommunityDtos.PageResponse;
import com.storyforge.domain.community.Post;
import com.storyforge.domain.community.Work;
import com.storyforge.domain.community.WorkRepository;
import com.storyforge.domain.project.Project;
import com.storyforge.domain.project.ProjectRepository;
import com.storyforge.exception.InvalidStateException;
import com.storyforge.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 작업 기록 저장과 조회. 작업 그래프(수동·에이전트)와 활동 잔디가 같은 기록을 씁니다.
 * docs/plan/community.md "작업 그래프"
 */
@Service
@Transactional(readOnly = true)
public class ActivityService {

    private static final Set<ActivityType> AUTHORING = Arrays.stream(ActivityType.values())
            .filter(t -> t.authoring())
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(ActivityType.class)));
    private static final int WEEKS = 53;

    private final ActivityEventRepository events;
    private final ActivityReviewRepository reviews;
    private final WorkRepository works;
    private final ProjectRepository projects;
    private final ApplicationEventPublisher publisher;
    private final ZoneId zone;

    public ActivityService(ActivityEventRepository events, ActivityReviewRepository reviews, WorkRepository works,
                           ProjectRepository projects, ApplicationEventPublisher publisher,
                           @Value("${storyforge.community.zone}") String zone) {
        this.events = events;
        this.reviews = reviews;
        this.works = works;
        this.projects = projects;
        this.publisher = publisher;
        this.zone = ZoneId.of(zone);
    }

    /** 프로젝트 작업 기록. 이미 게시한 프로젝트면 작품 그래프에도 이어집니다. */
    @Transactional
    public ActivityEvent recordProject(Project project, ActivityType type, String summary, int changedChars) {
        Long owner = project.getOwnerId();
        if (owner == null) {
            return null;
        }
        return recordFor(owner, project, type, summary, changedChars);
    }

    /** 작업을 한 사람 기준으로 남깁니다. 공동 창작자의 잔디에 집계하려면 이 메서드를 씁니다. */
    @Transactional
    public ActivityEvent recordFor(Long userId, Project project, ActivityType type, String summary, int changedChars) {
        if (userId == null) {
            return null;
        }
        Work work = works.findByProjectId(project.getId()).orElse(null);
        return save(new ActivityEvent(userId, project.getId(), work, null, type, summary, changedChars));
    }

    @Transactional
    public ActivityEvent recordWork(Long userId, Long projectId, Work work, ActivityType type, String summary, int changedChars) {
        return save(new ActivityEvent(userId, projectId, work, null, type, summary, changedChars));
    }

    @Transactional
    public void recordPost(Long userId, Post post, ActivityType type, String summary) {
        save(new ActivityEvent(userId, null, null, post, type, summary, 0));
    }

    /** 활동 기록 없이 업적만 다시 계산합니다 (추천·Clone을 받았을 때). */
    public void touchAchievements(Long userId) {
        publisher.publishEvent(new ActivityRecorded(userId));
    }

    private ActivityEvent save(ActivityEvent event) {
        ActivityEvent saved = events.save(event);
        publisher.publishEvent(new ActivityRecorded(saved.getUserId()));
        return saved;
    }

    /** 앞뒤 본문. 공개 목록에는 내려가지 않고 비교 화면에서만 읽습니다. */
    @Transactional
    public void remember(ActivityEvent event, String before, String after) {
        if (event == null || event.getId() == null) {
            return;
        }
        String left = clip(before);
        String right = clip(after);
        if (left == null && right == null) {
            return;
        }
        reviews.save(new ActivityReview(event.getId(), left, right));
    }

    public ReviewView review(Long projectId, Long eventId) {
        ActivityEvent event = events.findById(Objects.requireNonNull(eventId))
                .orElseThrow(() -> new NotFoundException("작업 기록을 찾을 수 없습니다."));
        if (!Objects.equals(projectId, event.getProjectId())) {
            throw new NotFoundException("작업 기록을 찾을 수 없습니다.");
        }
        ActivityReview review = reviews.findById(eventId).orElse(null);
        return new ReviewView(event.getId(), event.getSummary(),
                review == null ? null : review.getBeforeText(),
                review == null ? null : review.getAfterText());
    }

    private static String clip(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String trimmed = text.strip();
        return trimmed.length() <= 8000 ? trimmed : trimmed.substring(0, 8000);
    }

    public WorkGraph projectGraph(Long projectId) {
        return graph(events.findTop2000ByProjectIdAndTypeInOrderByCreatedAtDesc(projectId, AUTHORING),
                events.totalsByProject(projectId, AUTHORING));
    }

    public WorkGraph workGraph(Long workId) {
        return graph(events.findTop2000ByWorkIdAndTypeInOrderByCreatedAtDesc(workId, AUTHORING),
                events.totalsByWork(workId, AUTHORING));
    }

    private WorkGraph graph(List<ActivityEvent> list, List<ActivityEventRepository.ActorTotal> totals) {
        ActorShare manual = share(totals, ActivityType.Actor.MANUAL);
        ActorShare agent = share(totals, ActivityType.Actor.AGENT);
        Map<String, List<ActivityEvent>> byDay = list.stream()
                .collect(Collectors.groupingBy(e -> day(e.getCreatedAt()), TreeMap::new, Collectors.toList()));
        List<GraphDay> days = byDay.entrySet().stream().map(entry -> {
            int m = 0;
            int a = 0;
            long mc = 0;
            long ac = 0;
            for (ActivityEvent e : entry.getValue()) {
                if (e.getActor() == ActivityType.Actor.AGENT) {
                    a++;
                    ac += e.getChangedChars();
                } else {
                    m++;
                    mc += e.getChangedChars();
                }
            }
            return new GraphDay(entry.getKey(), m, a, mc, ac);
        }).toList();
        return new WorkGraph(manual, agent, days, list.stream().map(e -> entry(e, false, Map.of())).toList());
    }

    private static ActorShare share(List<ActivityEventRepository.ActorTotal> totals, ActivityType.Actor actor) {
        return totals.stream().filter(t -> t.getActor() == actor).findFirst()
                .map(t -> new ActorShare(t.getEvents(), t.getChars()))
                .orElse(new ActorShare(0, 0));
    }

    /** 최근 53주 활동 잔디. 활동이 많은 날일수록 level이 높습니다 (1~2건, 3~5건, 6~10건, 11건 이상). */
    public Contributions contributions(Long userId) {
        LocalDate today = LocalDate.now(zone);
        LocalDate from = today.minusWeeks(WEEKS - 1L).minusDays(today.getDayOfWeek().getValue() % 7L);
        List<ContributionDay> days = events.dailyCounts(userId, zone.getId(), from.atStartOfDay(zone).toInstant(),
                        today.plusDays(1).atStartOfDay(zone).toInstant()).stream()
                .map(d -> new ContributionDay(d.getDay(), (int) d.getCnt(), level(d.getCnt())))
                .toList();
        long total = days.stream().mapToLong(d -> d.count()).sum();
        Streaks streaks = streaks(userId);
        return new Contributions(from.toString(), today.toString(), total, streaks.current(), streaks.longest(), days);
    }

    static int level(long count) {
        if (count == 0) {
            return 0;
        }
        if (count <= 2) {
            return 1;
        }
        if (count <= 5) {
            return 2;
        }
        return count <= 10 ? 3 : 4;
    }

    public record Streaks(int current, int longest, int activeDays) {
    }

    public Streaks streaks(Long userId) {
        List<LocalDate> days = events.allDays(userId, zone.getId()).stream().map(d -> LocalDate.parse(d.getDay())).toList();
        int longest = 0;
        int run = 0;
        LocalDate previous = null;
        for (LocalDate d : days) {
            run = previous != null && previous.plusDays(1).equals(d) ? run + 1 : 1;
            longest = Math.max(longest, run);
            previous = d;
        }
        LocalDate today = LocalDate.now(zone);
        boolean alive = previous != null && !previous.isBefore(today.minusDays(1));
        return new Streaks(alive ? run : 0, longest, days.size());
    }

    /** 활동 내역. date(yyyy-MM-dd)를 주면 그날만 봅니다. */
    public PageResponse<ActivityEntry> feed(Long userId, boolean self, String date, int page, int size) {
        Instant from = Instant.EPOCH;
        Instant to = Instant.now().plusSeconds(60);
        if (date != null && !date.isBlank()) {
            LocalDate d;
            try {
                d = LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                throw new InvalidStateException("날짜 형식은 yyyy-MM-dd입니다.");
            }
            from = d.atStartOfDay(zone).toInstant();
            to = d.plusDays(1).atStartOfDay(zone).toInstant();
        }
        Page<ActivityEvent> result = events.feed(userId, self, from, to, PageRequest.of(page, size));
        Map<Long, Project> owned = self
                ? projects.findAllById(result.stream().map(e -> e.getProjectId()).filter(Objects::nonNull)
                        .collect(Collectors.toSet())).stream()
                        .collect(Collectors.toMap(p -> p.getId(), Function.identity()))
                : Map.of();
        return new PageResponse<>(result.stream().map(e -> entry(e, self, owned)).toList(),
                result.getTotalElements(), page, size);
    }

    private ActivityEntry entry(ActivityEvent e, boolean self, Map<Long, Project> owned) {
        Work w = e.getWork();
        Post p = e.getPost();
        Project project = self && e.getProjectId() != null ? owned.get(e.getProjectId()) : null;
        Work shown = w != null && (self || w.getVisibility() != Work.Visibility.PRIVATE) ? w : null;
        return new ActivityEntry(e.getId(), e.getType(), e.getActor(), e.getSummary(), e.getChangedChars(), e.getCreatedAt(),
                day(e.getCreatedAt()), shown == null ? null : shown.getId(), shown == null ? null : shown.getTitle(),
                p == null ? null : p.getId(), p == null ? null : p.getTitle(),
                project == null ? null : project.getId(), project == null ? null : project.getTitle());
    }

    private String day(Instant at) {
        return at.atZone(zone).toLocalDate().toString();
    }

    /** 두 글의 앞뒤 공통 부분을 뺀 변경 글자 수. 글자 수 비율 표시에만 쓰는 근사값입니다. */
    public static int changedChars(String before, String after) {
        String a = before == null ? "" : before;
        String b = after == null ? "" : after;
        int prefix = 0;
        int max = Math.min(a.length(), b.length());
        while (prefix < max && a.charAt(prefix) == b.charAt(prefix)) {
            prefix++;
        }
        int suffix = 0;
        while (suffix < max - prefix && a.charAt(a.length() - 1 - suffix) == b.charAt(b.length() - 1 - suffix)) {
            suffix++;
        }
        return Math.max(a.length(), b.length()) - prefix - suffix;
    }

    public static int length(String text) {
        return text == null ? 0 : text.length();
    }

    /** 게시 전 기록을 작품에 잇습니다. */
    @Transactional
    public void attach(Long projectId, Long workId) {
        events.attachToWork(projectId, workId);
    }
}
