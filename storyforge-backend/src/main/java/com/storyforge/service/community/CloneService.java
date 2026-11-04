package com.storyforge.service.community;

import com.storyforge.domain.activity.ActivityType;
import com.storyforge.domain.bible.BibleDtos.BibleSnapshot;
import com.storyforge.domain.bible.Character;
import com.storyforge.domain.chapter.Chapter;
import com.storyforge.domain.chapter.ChapterRepository;
import com.storyforge.domain.community.*;
import com.storyforge.domain.community.CommunityDtos.*;
import com.storyforge.domain.notification.Notification;
import com.storyforge.domain.project.Project;
import com.storyforge.domain.project.ProjectRepository;
import com.storyforge.domain.project.ProjectStatus;
import com.storyforge.domain.user.AppUser;
import com.storyforge.domain.user.AppUserRepository;
import com.storyforge.exception.InvalidStateException;
import com.storyforge.exception.NotFoundException;
import com.storyforge.service.bible.BibleService;
import com.storyforge.service.chapter.ChapterService;
import com.storyforge.service.notification.NotificationService;
import com.storyforge.service.project.OutlineService;
import com.storyforge.service.project.ProjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Clone: 작품의 창작 자료를 내 프로젝트로 가져옵니다. 공유가 "읽을 링크 전달"이라면 Clone은 "이어서 쓸 작업본 만들기"입니다.
 * 범위에 따라 파이프라인 시작 상태가 다릅니다. 전이 규칙은 그대로이며 시작 상태만 정합니다.
 * WORLD → BIBLE_DRAFT, PLOT → OUTLINE_DRAFT, FULL → CHAPTER_REVIEW (복제 원고도 앞 장부터 다시 승인)
 */
@Service
@Transactional(readOnly = true)
public class CloneService {

    private static final int MAX_NODES = 200;

    private final WorkService workService;
    private final WorkRepository works;
    private final WorkChapterRepository workChapters;
    private final WorkCloneRepository clones;
    private final ProjectRepository projects;
    private final ProjectService projectService;
    private final BibleService bibles;
    private final OutlineService outlines;
    private final ChapterService chapterService;
    private final ChapterRepository chapters;
    private final AppUserRepository users;
    private final ActivityService activity;
    private final IssueService issues;
    private final NotificationService notifications;

    public CloneService(WorkService workService, WorkRepository works, WorkChapterRepository workChapters,
                        WorkCloneRepository clones, ProjectRepository projects, ProjectService projectService,
                        BibleService bibles, OutlineService outlines, ChapterService chapterService,
                        ChapterRepository chapters, AppUserRepository users, ActivityService activity, IssueService issues,
                        NotificationService notifications) {
        this.notifications = notifications;
        this.workService = workService;
        this.works = works;
        this.workChapters = workChapters;
        this.clones = clones;
        this.projects = projects;
        this.projectService = projectService;
        this.bibles = bibles;
        this.outlines = outlines;
        this.chapterService = chapterService;
        this.chapters = chapters;
        this.users = users;
        this.activity = activity;
        this.issues = issues;
    }

    @Transactional
    public CloneResponse cloneWork(Long workId, CloneRequest request, Long userId) {
        Work work = workService.loadVisible(workId, userId);
        boolean owned = work.getAuthor().getId().equals(userId);
        if (!WorkService.cloneModes(work, owned).contains(request.mode())) {
            throw new InvalidStateException("이 작품은 해당 범위로 Clone할 수 없습니다.");
        }
        AppUser user = users.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        String title = WorkService.blankToNull(request.title());
        ProjectStatus start = switch (request.mode()) {
            case WORLD -> ProjectStatus.BIBLE_DRAFT;
            case PLOT -> ProjectStatus.OUTLINE_DRAFT;
            case FULL -> ProjectStatus.CHAPTER_REVIEW;
        };
        Project project = projects.save(Project.cloned(title == null ? work.getTitle() + " (Clone)" : title,
                work.getGenre(), userId, start, work.getKind() == Work.Kind.SHORT ? Project.Form.SHORT : Project.Form.LONG,
                work.getId(), work.getTitle(), work.getRevision(), request.mode().name()));

        BibleSnapshot bible = work.getBibleSnapshot();
        bibles.replace(project, bible.worldSetting(), bible.theme(), bible.tone(), bible.characters().stream()
                .map(c -> new Character(c.name(), c.description(), c.personality(), c.relationships(), null))
                .toList());
        if (request.mode() != WorkClone.Mode.WORLD) {
            outlines.replace(project, work.getOutlineSnapshot());
        }
        if (request.mode() == WorkClone.Mode.FULL) {
            for (WorkChapter c : workChapters.findByWorkIdOrderByNumber(workId)) {
                chapterService.importDraft(project, c.getNumber(), c.getTitle(), c.getContent(), c.getSummary(), workId);
            }
        }
        clones.save(new WorkClone(workId, user, project.getId(), request.mode(), work.getRevision()));
        works.addClone(workId);
        issues.reached(workId, IssueEvent.Metric.TOTAL_CLONES, works.cloneCount(workId));
        activity.recordWork(userId, project.getId(), work, ActivityType.PROJECT_CLONED,
                modeLabel(request.mode()) + " 범위로 Clone (r" + work.getRevision() + ")", 0);
        activity.touchAchievements(work.getAuthor().getId());
        notifications.notify(work.getAuthor().getId(), userId, Notification.Type.CLONE,
                user.getDisplayName() + "님이 「" + work.getTitle() + "」을(를) Clone했습니다.", "/works/" + work.getId());
        return new CloneResponse(project.getId(), request.mode(), work.getRevision());
    }

    private static String modeLabel(WorkClone.Mode mode) {
        return switch (mode) {
            case WORLD -> "세계관";
            case PLOT -> "플롯";
            case FULL -> "전체 원고";
        };
    }

    /** Clone 네트워크: 뿌리 작품부터 게시된 파생작 트리와 이 작품의 Clone 기록 */
    public CloneNetwork network(Long workId, Long viewerId) {
        Work work = workService.loadVisible(workId, viewerId);
        Work root = work;
        Set<Long> seen = new HashSet<>(List.of(work.getId()));
        while (root.getParentWorkId() != null && seen.add(root.getParentWorkId())) {
            Work parent = works.findWithAuthor(root.getParentWorkId()).orElse(null);
            if (parent == null || !parent.isListed()) {
                break;
            }
            root = parent;
        }
        List<NetworkNode> nodes = new ArrayList<>();
        nodes.add(node(root, null, workId));
        Set<Long> visited = new HashSet<>(List.of(root.getId()));
        List<Long> frontier = List.of(root.getId());
        while (!frontier.isEmpty() && nodes.size() < MAX_NODES) {
            List<Long> next = new ArrayList<>();
            for (Work child : works.findByParentWorkIdIn(frontier)) {
                boolean listed = child.isListed() || child.getId().equals(workId);
                if (listed && visited.add(child.getId()) && nodes.size() < MAX_NODES) {
                    nodes.add(node(child, child.getParentWorkId(), workId));
                    next.add(child.getId());
                }
            }
            frontier = next;
        }
        List<WorkClone> recent = clones.findTop50ByWorkIdOrderByCreatedAtDesc(workId);
        Set<Long> projectIds = recent.stream().map(c -> c.getProjectId()).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Long> derived = new HashMap<>();
        for (Long projectId : projectIds) {
            works.findByProjectId(projectId).filter(Work::isListed)
                    .ifPresent(w -> derived.put(projectId, w.getId()));
        }
        List<CloneEntry> entries = recent.stream()
                .map(c -> new CloneEntry(UserSummary.from(c.getUser()), c.getMode(), c.getRevision(), c.getCreatedAt(),
                        c.getProjectId() == null ? null : derived.get(c.getProjectId())))
                .toList();
        return new CloneNetwork(root.getId(), nodes, clones.countByWorkIdAndMode(workId, WorkClone.Mode.WORLD),
                clones.countByWorkIdAndMode(workId, WorkClone.Mode.PLOT),
                clones.countByWorkIdAndMode(workId, WorkClone.Mode.FULL), entries);
    }

    private static NetworkNode node(Work w, Long parentId, Long currentId) {
        return new NetworkNode(w.getId(), parentId, w.getTitle(), UserSummary.from(w.getAuthor()), w.getRevision(),
                w.getLikeCount(), w.getCloneCount(), w.getId().equals(currentId));
    }

    /** 원본 비교: 내 Clone과 원본 최신 게시본의 챕터별 차이. 원본이 사라졌거나 비공개면 available=false입니다. */
    public UpstreamCompare compare(Long projectId, Long userId) {
        Project project = projectService.load(projectId);
        if (project.getClonedFromRevision() == null) {
            throw new NotFoundException("Clone한 프로젝트가 아닙니다.");
        }
        WorkClone.Mode mode = WorkClone.Mode.valueOf(project.getCloneMode());
        Work upstream = project.getClonedFromWorkId() == null ? null
                : works.findWithAuthor(project.getClonedFromWorkId())
                .filter(w -> (w.getVisibility() != Work.Visibility.PRIVATE && !w.isHidden()) || w.getAuthor().getId().equals(userId))
                .orElse(null);
        if (upstream == null) {
            return new UpstreamCompare(null, project.getClonedFromTitle(), mode, project.getClonedFromRevision(),
                    project.getClonedFromRevision(), false, List.of());
        }
        Map<Integer, WorkChapter> theirs = workChapters.findByWorkIdOrderByNumber(upstream.getId()).stream()
                .collect(Collectors.toMap(c -> c.getNumber(), Function.identity()));
        Map<Integer, Chapter> ours = chapters.findByProjectIdOrderByNumber(projectId).stream()
                .collect(Collectors.toMap(c -> c.getNumber(), Function.identity()));
        List<UpstreamChapter> rows = new TreeSet<>(union(theirs.keySet(), ours.keySet())).stream()
                .map(n -> row(n, theirs.get(n), ours.get(n)))
                .toList();
        return new UpstreamCompare(upstream.getId(), upstream.getTitle(), mode, project.getClonedFromRevision(),
                upstream.getRevision(), true, rows);
    }

    /** 원본 최신 게시본의 한 챕터를 내 Draft로 가져옵니다. 전체 원고 Clone이 검토 단계일 때만 됩니다. */
    @Transactional
    public UpstreamCompare pullChapter(Long projectId, int number, Long userId) {
        Project project = projectService.load(projectId);
        if (!WorkClone.Mode.FULL.name().equals(project.getCloneMode())) {
            throw new InvalidStateException("전체 원고 Clone만 원본 챕터를 가져올 수 있습니다.");
        }
        if (project.getStatus() != ProjectStatus.CHAPTER_REVIEW) {
            throw new InvalidStateException("챕터 검토 단계에서만 가져올 수 있습니다.");
        }
        Work upstream = readableUpstream(project, userId);
        boolean owned = upstream.getAuthor().getId().equals(userId);
        if (!WorkService.cloneModes(upstream, owned).contains(WorkClone.Mode.FULL)) {
            throw new InvalidStateException("작가가 전체 원고 Clone을 허용하지 않아 가져올 수 없습니다.");
        }
        WorkChapter theirs = workChapters.findByWorkIdAndNumber(upstream.getId(), number)
                .orElseThrow(() -> new NotFoundException("원본에 없는 챕터입니다: " + number));
        if (number > outlines.get(projectId).getChapters().size()) {
            throw new InvalidStateException("내 Outline에 없는 챕터입니다: " + number);
        }
        chapterService.pullFromUpstream(project, number, theirs.getTitle(), theirs.getContent(), theirs.getSummary(), upstream.getId());
        return compare(projectId, userId);
    }

    /** 원본의 현재 리비전까지 확인했다고 표시합니다. 내 원고는 바꾸지 않습니다. */
    @Transactional
    public UpstreamCompare acknowledge(Long projectId, Long userId) {
        Project project = projectService.load(projectId);
        project.acknowledgeUpstream(readableUpstream(project, userId).getRevision());
        return compare(projectId, userId);
    }

    private Work readableUpstream(Project project, Long userId) {
        if (project.getClonedFromRevision() == null || project.getClonedFromWorkId() == null) {
            throw new InvalidStateException("원본이 없어 할 수 없습니다.");
        }
        return works.findWithAuthor(project.getClonedFromWorkId())
                .filter(w -> (w.getVisibility() != Work.Visibility.PRIVATE && !w.isHidden()) || w.getAuthor().getId().equals(userId))
                .orElseThrow(() -> new InvalidStateException("원본이 비공개로 바뀌어 할 수 없습니다."));
    }

    private static Set<Integer> union(Set<Integer> a, Set<Integer> b) {
        Set<Integer> all = new HashSet<>(a);
        all.addAll(b);
        return all;
    }

    private static UpstreamChapter row(int number, WorkChapter theirs, Chapter ours) {
        String theirText = theirs == null ? null : theirs.getContent();
        String ourText = ours == null ? null : ours.getContent();
        String state;
        if (theirs == null) {
            state = "ONLY_LOCAL";
        } else if (ours == null) {
            state = "ONLY_UPSTREAM";
        } else {
            state = Objects.equals(theirText, ourText) && Objects.equals(theirs.getTitle(), ours.getTitle()) ? "SAME" : "CHANGED";
        }
        return new UpstreamChapter(number, theirs == null ? null : theirs.getTitle(), ours == null ? null : ours.getTitle(),
                state, ActivityService.changedChars(theirText, ourText), theirText, ourText);
    }
}
