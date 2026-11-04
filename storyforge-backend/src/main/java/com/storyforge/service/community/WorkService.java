package com.storyforge.service.community;

import com.storyforge.domain.activity.ActivityType;
import com.storyforge.domain.bible.BibleDtos.BibleSnapshot;
import com.storyforge.domain.chapter.Chapter;
import com.storyforge.domain.chapter.ChapterRepository;
import com.storyforge.domain.chapter.ChapterStatus;
import com.storyforge.domain.community.*;
import com.storyforge.domain.community.CommunityDtos.*;
import com.storyforge.domain.outline.OutlineChapter;
import com.storyforge.domain.outline.OutlineRepository;
import com.storyforge.domain.project.Project;
import com.storyforge.domain.user.AppUser;
import com.storyforge.domain.user.AppUserRepository;
import com.storyforge.exception.InvalidStateException;
import com.storyforge.exception.NotFoundException;
import com.storyforge.service.auth.AccountPolicy;
import com.storyforge.service.bible.BibleService;
import com.storyforge.service.project.ExportService;
import com.storyforge.service.project.ProjectService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 작품 게시와 조회. 승인된 챕터만 게시본에 들어갑니다 (사용자 검토 원칙).
 * 게시본은 프로젝트 파이프라인 상태를 바꾸지 않습니다.
 */
@Service
@Transactional(readOnly = true)
public class WorkService {

    private static final Map<String, Sort> SORTS = Map.of(
            "latest", Sort.by(Sort.Direction.DESC, "publishedAt"),
            "popular", Sort.by(Sort.Direction.DESC, "likeCount", "publishedAt"),
            "views", Sort.by(Sort.Direction.DESC, "viewCount", "publishedAt"),
            "clones", Sort.by(Sort.Direction.DESC, "cloneCount", "publishedAt"));

    private final WorkRepository works;
    private final WorkChapterRepository workChapters;
    private final WorkRevisionRepository revisions;
    private final ProjectService projects;
    private final ChapterRepository chapters;
    private final BibleService bibles;
    private final OutlineRepository outlines;
    private final AppUserRepository users;
    private final PackageItemRepository packageItems;
    private final PackageService packages;
    private final ActivityService activity;
    private final IssueService issues;
    private final ExportService exports;
    private final JdbcTemplate jdbc;
    private final AccountPolicy policy;
    private final ContentPolicy contentPolicy;

    public WorkService(WorkRepository works, WorkChapterRepository workChapters, WorkRevisionRepository revisions,
                       ProjectService projects, ChapterRepository chapters, BibleService bibles, OutlineRepository outlines,
                       AppUserRepository users, PackageItemRepository packageItems, PackageService packages,
                       ActivityService activity, IssueService issues, ExportService exports, JdbcTemplate jdbc,
                       AccountPolicy policy, ContentPolicy contentPolicy) {
        this.contentPolicy = contentPolicy;
        this.policy = policy;
        this.works = works;
        this.workChapters = workChapters;
        this.revisions = revisions;
        this.projects = projects;
        this.chapters = chapters;
        this.bibles = bibles;
        this.outlines = outlines;
        this.users = users;
        this.packageItems = packageItems;
        this.packages = packages;
        this.activity = activity;
        this.issues = issues;
        this.exports = exports;
        this.jdbc = jdbc;
    }

    // ───────────── 게시판 ─────────────

    public PageResponse<WorkCard> board(Work.Kind kind, String genre, String q, String sort, int page, int size, Long viewerId) {
        Sort order = SORTS.getOrDefault(sort == null ? "latest" : sort, SORTS.get("latest"));
        Page<Work> result = works.board(kind, genre == null ? "" : genre.strip(), likePattern(q), viewerId == null ? -1L : viewerId,
                PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 50), order));
        return new PageResponse<>(result.stream().map(WorkCard::from).toList(), result.getTotalElements(),
                result.getNumber(), result.getSize());
    }

    /** LIKE 패턴. 사용자가 입력한 %, _는 글자 그대로 찾습니다. 없으면 빈 문자열입니다. */
    static String likePattern(String q) {
        if (q == null || q.isBlank()) {
            return "";
        }
        String escaped = q.strip().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    public List<WorkCard> mine(Long userId) {
        return works.findByAuthorIdOrderByUpdatedAtDesc(userId).stream().map(WorkCard::from).toList();
    }

    public List<WorkCard> publicWorksOf(Long userId) {
        return works.findByAuthorIdAndVisibilityAndHiddenAtIsNullOrderByPublishedAtDesc(userId, Work.Visibility.PUBLIC).stream()
                .map(WorkCard::from).toList();
    }

    // ───────────── 조회 ─────────────

    /** 비공개 작품은 작가 외에는 없는 것처럼 404입니다. */
    public Work loadVisible(Long workId, Long viewerId) {
        Work work = works.findWithAuthor(Objects.requireNonNull(workId))
                .orElseThrow(() -> new NotFoundException("작품을 찾을 수 없습니다: " + workId));
        boolean author = work.getAuthor().getId().equals(viewerId);
        if (work.getVisibility() == Work.Visibility.PRIVATE && !author) {
            throw new NotFoundException("작품을 찾을 수 없습니다: " + workId);
        }
        // 관리자가 숨긴 작품은 작가와 관리자만 봅니다.
        if (work.isHidden() && !author && !policy.isAdminId(viewerId)) {
            throw new NotFoundException("작품을 찾을 수 없습니다: " + workId);
        }
        return work;
    }

    /** 수정 권한이 없으면 404입니다 (프로젝트 소유자 검사와 같은 규칙). */
    public Work loadOwned(Long workId, Long userId) {
        Work work = works.findWithAuthor(Objects.requireNonNull(workId))
                .orElseThrow(() -> new NotFoundException("작품을 찾을 수 없습니다: " + workId));
        if (!work.getAuthor().getId().equals(userId)) {
            throw new NotFoundException("작품을 찾을 수 없습니다: " + workId);
        }
        return work;
    }

    @Transactional
    public WorkDetail detail(Long workId, Long viewerId) {
        Work work = loadVisible(workId, viewerId);
        boolean owned = work.getAuthor().getId().equals(viewerId);
        if (!owned) {
            works.addView(workId);
            issues.reached(workId, IssueEvent.Metric.TOTAL_VIEWS, works.viewCount(workId));
        }
        boolean liked = viewerId != null && Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM work_like WHERE work_id = ? AND user_id = ?)", Boolean.class, workId, viewerId));
        List<ChapterEntry> entries = workChapters.findByWorkIdOrderByNumber(workId).stream()
                .map(c -> new ChapterEntry(c.getNumber(), c.getTitle(), ActivityService.length(c.getContent())))
                .toList();
        List<WorkClone.Mode> modes = cloneModes(work, owned);
        boolean sourceVisible = !modes.isEmpty();
        List<Long> packageIds = packageItems.findPackageIdsOf(workId);
        List<WorkCard> inPackages = packageIds.isEmpty() ? List.of() : works.findAllWithAuthor(packageIds).stream()
                .filter(p -> p.isListed() || p.getAuthor().getId().equals(viewerId))
                .map(WorkCard::from).toList();
        return new WorkDetail(WorkCard.from(work), owned, liked, work.isDownloadable(), owned ? work.getProjectId() : null,
                modes, entries,
                sourceVisible ? work.getBibleSnapshot() : null,
                sourceVisible && modes.contains(WorkClone.Mode.PLOT) ? work.getOutlineSnapshot() : null,
                revisions.findByWorkIdOrderByRevisionDesc(workId).stream().map(RevisionEntry::from).toList(),
                work.getKind() == Work.Kind.PACKAGE ? packages.view(work, viewerId) : null,
                inPackages);
    }

    /** 이 사용자가 고를 수 있는 Clone 범위. 작가 본인은 정책과 관계없이 모두 가능합니다. */
    static List<WorkClone.Mode> cloneModes(Work work, boolean owned) {
        if (work.getKind() == Work.Kind.PACKAGE || work.getBibleSnapshot() == null) {
            return List.of();
        }
        boolean hasOutline = work.getOutlineSnapshot() != null && !work.getOutlineSnapshot().isEmpty();
        List<WorkClone.Mode> all = hasOutline
                ? List.of(WorkClone.Mode.WORLD, WorkClone.Mode.PLOT, WorkClone.Mode.FULL)
                : List.of(WorkClone.Mode.WORLD);
        if (owned || work.getClonePolicy() == Work.ClonePolicy.OPEN) {
            return all;
        }
        return work.getClonePolicy() == Work.ClonePolicy.WORLD_ONLY ? List.of(WorkClone.Mode.WORLD) : List.of();
    }

    public WorkChapterResponse chapter(Long workId, int number, Long viewerId) {
        Work work = loadVisible(workId, viewerId);
        List<WorkChapter> list = workChapters.findByWorkIdOrderByNumber(work.getId());
        for (int i = 0; i < list.size(); i++) {
            WorkChapter c = list.get(i);
            if (c.getNumber() == number) {
                return new WorkChapterResponse(c.getNumber(), c.getTitle(), c.getContent(),
                        i > 0 ? list.get(i - 1).getNumber() : null,
                        i + 1 < list.size() ? list.get(i + 1).getNumber() : null);
            }
        }
        throw new NotFoundException("챕터를 찾을 수 없습니다: " + number);
    }

    // ───────────── 게시·수정 ─────────────

    /** POST /projects/{id}/publish. 승인된 챕터가 있어야 합니다. 프로젝트 하나에 작품 하나입니다. */
    @Transactional
    public WorkCard publish(Long projectId, PublishRequest request, Long userId) {
        if (request.kind() == Work.Kind.PACKAGE) {
            throw new InvalidStateException("패키지는 패키지포지에서 만듭니다.");
        }
        policy.requireTerms(userId);
        Project project = projects.load(projectId);
        if (!userId.equals(project.getOwnerId())) {
            throw new InvalidStateException("게시할 수 있는 사람은 작품 주인뿐입니다.");
        }
        if (works.findIdByProjectId(projectId).isPresent()) {
            throw new InvalidStateException("이미 게시한 프로젝트입니다. 게시본 업데이트를 사용하세요.");
        }
        List<Chapter> confirmed = confirmedChapters(projectId);
        if (confirmed.isEmpty()) {
            throw new InvalidStateException("승인된 챕터가 있어야 게시할 수 있습니다.");
        }
        contentPolicy.check(request.title(), request.synopsis());
        checkChapters(confirmed);
        AppUser author = users.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        Work work = new Work(author, request.kind(), request.title().strip());
        work.updateMeta(request.title().strip(), blankToNull(request.synopsis()), blankToNull(request.genre()),
                blankToNull(request.tags()), request.visibility(), request.clonePolicy(), request.downloadable());
        work.linkSource(projectId, project.getClonedFromWorkId(), project.getClonedFromTitle());
        applySource(work, projectId);
        work = works.save(work);
        int chars = copyChapters(work, confirmed);
        work.applyCounts(confirmed.size(), chars);
        revisions.save(new WorkRevision(work.getId(), 1, "첫 게시", confirmed.size(), chars));
        activity.attach(projectId, work.getId());
        activity.recordWork(userId, projectId, work, ActivityType.WORK_PUBLISHED, "작품 게시", 0);
        return WorkCard.from(work);
    }

    /** 프로젝트에서 승인된 챕터를 다시 가져와 게시본을 업데이트합니다. */
    @Transactional
    public WorkCard sync(Long workId, SyncWorkRequest request, Long userId) {
        Work work = loadOwned(workId, userId);
        if (work.getProjectId() == null) {
            throw new InvalidStateException("연결된 프로젝트가 없어 가져올 수 없습니다.");
        }
        List<Chapter> confirmed = confirmedChapters(work.getProjectId());
        if (confirmed.isEmpty()) {
            throw new InvalidStateException("승인된 챕터가 있어야 업데이트할 수 있습니다.");
        }
        checkChapters(confirmed);
        applySource(work, work.getProjectId());
        int chars = copyChapters(work, confirmed);
        work.applyCounts(confirmed.size(), chars);
        int revision = work.nextRevision();
        String message = blankToNull(request.message());
        revisions.save(new WorkRevision(workId, revision, message == null ? "프로젝트에서 업데이트" : message, confirmed.size(), chars));
        activity.recordWork(userId, work.getProjectId(), work, ActivityType.WORK_SYNCED, "프로젝트에서 게시본 업데이트 (r" + revision + ")", 0);
        return WorkCard.from(work);
    }

    @Transactional
    public WorkCard update(Long workId, UpdateWorkRequest request, Long userId) {
        contentPolicy.check(request.title(), request.synopsis(), request.worldNote());
        Work work = loadOwned(workId, userId);
        work.updateMeta(request.title().strip(), blankToNull(request.synopsis()), blankToNull(request.genre()),
                blankToNull(request.tags()), request.visibility(), request.clonePolicy(), request.downloadable());
        if (work.getKind() == Work.Kind.PACKAGE) {
            work.updatePackage(request.packageType() == null ? work.getPackageType() : request.packageType(),
                    blankToNull(request.worldNote()));
        }
        activity.recordWork(userId, work.getProjectId(), work, ActivityType.WORK_UPDATED, "작품 정보 수정", 0);
        return WorkCard.from(work);
    }

    /** 게시본 본문 직접 수정. 프로젝트 원고는 건드리지 않습니다. */
    @Transactional
    public WorkChapterResponse editChapter(Long workId, int number, UpdateWorkChapterRequest request, Long userId) {
        contentPolicy.check(request.title(), request.content());
        Work work = loadOwned(workId, userId);
        WorkChapter chapter = workChapters.findByWorkIdAndNumber(workId, number)
                .orElseThrow(() -> new NotFoundException("챕터를 찾을 수 없습니다: " + number));
        int changed = ActivityService.changedChars(chapter.getContent(), request.content())
                + ActivityService.changedChars(chapter.getTitle(), request.title());
        if (changed > 0) {
            chapter.edit(blankToNull(request.title()), request.content());
            List<WorkChapter> all = workChapters.findByWorkIdOrderByNumber(workId);
            int chars = all.stream().mapToInt(c -> ActivityService.length(c.getContent())).sum();
            work.applyCounts(all.size(), chars);
            int revision = work.nextRevision();
            String message = blankToNull(request.message());
            revisions.save(new WorkRevision(workId, revision, message == null ? number + "장 수정" : message, all.size(), chars));
            activity.recordWork(userId, work.getProjectId(), work, ActivityType.WORK_CHAPTER_EDITED,
                    "게시본 " + number + "장 직접 수정 (r" + revision + ")", changed);
        }
        return chapter(workId, number, userId);
    }

    @Transactional
    public void delete(Long workId, Long userId) {
        works.delete(Objects.requireNonNull(loadOwned(workId, userId)));
    }

    // ───────────── 내보내기 ─────────────

    public ExportService.ExportFile export(Long workId, String format, Long viewerId) {
        Work work = loadVisible(workId, viewerId);
        boolean owned = work.getAuthor().getId().equals(viewerId);
        if (!work.isDownloadable() && !owned) {
            throw new InvalidStateException("작가가 내려받기를 허용하지 않은 작품입니다.");
        }
        List<ExportService.ManuscriptChapter> sections = new ArrayList<>();
        if (work.getKind() == Work.Kind.PACKAGE) {
            for (PackageItem item : packageItems.findWithMembers(workId)) {
                Work member = item.getMember();
                boolean memberOwned = member.getAuthor().getId().equals(viewerId);
                if (item.getStatus() == PackageItem.Status.APPROVED && member.getVisibility() != Work.Visibility.PRIVATE && !member.isHidden()
                        && (member.isDownloadable() || memberOwned)) {
                    sections.addAll(sectionsOf(member, member.getTitle() + " — "));
                }
            }
        } else {
            sections.addAll(sectionsOf(work, ""));
        }
        if (sections.isEmpty()) {
            throw new InvalidStateException("내보낼 내용이 없습니다.");
        }
        return exports.render(new ExportService.Manuscript(work.getTitle(), work.getAuthor().getDisplayName(), sections), format);
    }

    private List<ExportService.ManuscriptChapter> sectionsOf(Work work, String prefix) {
        return workChapters.findByWorkIdOrderByNumber(work.getId()).stream()
                .map(c -> new ExportService.ManuscriptChapter(
                        prefix + c.getNumber() + "장. " + (c.getTitle() == null ? "" : c.getTitle()), c.getContent()))
                .toList();
    }

    // ───────────── helpers ─────────────

    private void checkChapters(List<Chapter> confirmed) {
        for (Chapter chapter : confirmed) {
            contentPolicy.check(chapter.getTitle(), chapter.getContent());
        }
    }

    private List<Chapter> confirmedChapters(Long projectId) {
        return chapters.findByProjectIdOrderByNumber(projectId).stream()
                .filter(c -> c.getStatus() == ChapterStatus.CONFIRMED)
                .toList();
    }

    private void applySource(Work work, Long projectId) {
        BibleSnapshot bible = bibles.find(projectId).map(BibleSnapshot::from).orElse(null);
        List<OutlineChapter> outline = outlines.findByProjectId(projectId)
                .map(o -> List.copyOf(o.getChapters())).orElse(null);
        work.applySnapshot(bible, outline);
    }

    /** 번호가 같으면 갱신하고, 빠진 챕터는 지웁니다. 글자 수 합계를 돌려줍니다. */
    private int copyChapters(Work work, List<Chapter> confirmed) {
        Map<Integer, WorkChapter> existing = workChapters.findByWorkIdOrderByNumber(work.getId()).stream()
                .collect(Collectors.toMap(c -> c.getNumber(), Function.identity()));
        int chars = 0;
        for (Chapter c : confirmed) {
            WorkChapter target = existing.remove(c.getNumber());
            if (target == null) {
                target = new WorkChapter(work, c.getNumber());
            }
            target.apply(c.getTitle(), c.getContent(), c.getSummary());
            workChapters.save(target);
            chars += ActivityService.length(c.getContent());
        }
        workChapters.deleteAll(Objects.requireNonNull(existing.values()));
        return chars;
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
