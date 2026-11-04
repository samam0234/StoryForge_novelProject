package com.storyforge.service.orchestration;

import com.storyforge.client.AiDtos.*;
import com.storyforge.client.AiServiceClient;
import com.storyforge.domain.activity.ActivityType;
import com.storyforge.domain.bible.BibleDtos.BibleResponse;
import com.storyforge.domain.bible.BibleDtos.BibleSnapshot;
import com.storyforge.domain.bible.BibleDtos.UpdateBibleRequest;
import com.storyforge.domain.bible.Character;
import com.storyforge.domain.bible.StoryBible;
import com.storyforge.domain.chapter.Chapter;
import com.storyforge.domain.chapter.ChapterDtos.ChapterResponse;
import com.storyforge.domain.chapter.ChapterStatus;
import com.storyforge.domain.outline.Outline;
import com.storyforge.domain.outline.OutlineChapter;
import com.storyforge.domain.outline.OutlineDtos.OutlineResponse;
import com.storyforge.domain.project.Project;
import com.storyforge.domain.project.ProjectStatus;
import com.storyforge.exception.AiServiceException;
import com.storyforge.exception.InvalidStateException;
import com.storyforge.service.bible.BibleService;
import com.storyforge.service.chapter.ChapterService;
import com.storyforge.service.community.ActivityService;
import com.storyforge.service.project.OutlineService;
import com.storyforge.service.project.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.storyforge.domain.project.ProjectStatus.*;

/**
 * 파이프라인 오케스트레이션 (기획서 §7.2).
 * DB 작업은 짧은 트랜잭션으로 나누고, FastAPI 호출은 트랜잭션 밖에서 합니다.
 */
@Service
public class PipelineOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(PipelineOrchestrator.class);

    private final PipelineStateMachine stateMachine;
    private final ProjectService projectService;
    private final BibleService bibleService;
    private final OutlineService outlineService;
    private final ChapterService chapterService;
    private final AiServiceClient ai;
    private final TransactionTemplate tx;
    private final ObjectMapper objectMapper;
    private final ActivityService activity;

    public PipelineOrchestrator(PipelineStateMachine stateMachine, ProjectService projectService,
                                BibleService bibleService, OutlineService outlineService,
                                ChapterService chapterService, AiServiceClient ai,
                                TransactionTemplate tx, ObjectMapper objectMapper, ActivityService activity) {
        this.activity = activity;
        this.stateMachine = stateMachine;
        this.projectService = projectService;
        this.bibleService = bibleService;
        this.outlineService = outlineService;
        this.chapterService = chapterService;
        this.ai = ai;
        this.tx = tx;
        this.objectMapper = objectMapper;
    }

    // ───────────── Story Bible ─────────────

    public BibleResponse generateBible(Long projectId, String prompt) {
        ProjectInfo info = inTx(s -> {
            Project p = projectService.load(projectId);
            stateMachine.requireIn(p, BIBLE_DRAFT);
            return projectInfo(p);
        });
        BibleGenerateResponse generated = ai.generateBible(new BibleGenerateRequest(info, prompt));
        return inTx(s -> {
            Project p = projectService.load(projectId);
            stateMachine.requireIn(p, BIBLE_DRAFT);
            List<Character> characters = generated.characters().stream()
                    .map(c -> new Character(c.name(), c.description(), c.personality(), c.relationships(),
                            c.currentState()))
                    .toList();
            StoryBible bible = bibleService.replace(p, generated.worldSetting(), generated.theme(),
                    generated.tone(), characters);
            activity.recordProject(p, ActivityType.BIBLE_GENERATED, "Story Bible AI 생성 (v" + bible.getVersion() + ")",
                    ActivityService.length(bibleText(bible)));
            return BibleResponse.from(bible);
        });
    }

    public BibleResponse updateBible(Long projectId, UpdateBibleRequest request) {
        return inTx(s -> {
            Project p = projectService.load(projectId);
            stateMachine.requireIn(p, BIBLE_DRAFT);
            List<Character> characters = request.characters() == null ? List.of()
                    : request.characters().stream().map(c -> c.toEntity()).toList();
            String before = bibleService.find(projectId).map(PipelineOrchestrator::bibleText).orElse("");
            StoryBible bible = bibleService.replace(p, request.worldSetting(), request.theme(), request.tone(), characters);
            int changed = ActivityService.changedChars(before, bibleText(bible));
            if (changed > 0) {
                activity.recordProject(p, ActivityType.BIBLE_EDITED, "Story Bible 직접 수정 (v" + bible.getVersion() + ")", changed);
            }
            return BibleResponse.from(bible);
        });
    }

    public void confirmBible(Long projectId) {
        tx.executeWithoutResult(s -> {
            Project p = projectService.load(projectId);
            bibleService.get(projectId);
            stateMachine.transition(p, BIBLE_CONFIRMED);
            activity.recordProject(p, ActivityType.BIBLE_CONFIRMED, "Story Bible 확정", 0);
        });
    }

    // ───────────── Outline ─────────────

    public OutlineResponse generateOutline(Long projectId, int chapterCount) {
        OutlineGenerateRequest request = inTx(s -> {
            Project p = projectService.load(projectId);
            stateMachine.requireIn(p, BIBLE_CONFIRMED, OUTLINE_DRAFT);
            return new OutlineGenerateRequest(projectInfo(p), bibleInfo(bibleService.get(projectId)), chapterCount);
        });
        OutlineGenerateResponse generated = ai.generateOutline(request);
        return inTx(s -> {
            Project p = projectService.load(projectId);
            stateMachine.requireIn(p, BIBLE_CONFIRMED, OUTLINE_DRAFT);
            Outline outline = outlineService.replace(p, generated.chapters());
            if (p.getStatus() == BIBLE_CONFIRMED) {
                stateMachine.transition(p, OUTLINE_DRAFT);
            }
            activity.recordProject(p, ActivityType.OUTLINE_GENERATED, "Outline AI 생성 (" + generated.chapters().size() + "장)",
                    ActivityService.length(generated.chapters().toString()));
            return OutlineResponse.from(outline);
        });
    }

    public OutlineResponse updateOutline(Long projectId, List<OutlineChapter> chapters) {
        return inTx(s -> {
            Project p = projectService.load(projectId);
            stateMachine.requireIn(p, OUTLINE_DRAFT);
            p.touch();
            String before = outlineService.get(projectId).getChapters().toString();
            Outline outline = outlineService.replace(p, chapters);
            int changed = ActivityService.changedChars(before, outline.getChapters().toString());
            if (changed > 0) {
                activity.recordProject(p, ActivityType.OUTLINE_EDITED, "Outline 직접 수정", changed);
            }
            return OutlineResponse.from(outline);
        });
    }

    public void confirmOutline(Long projectId) {
        tx.executeWithoutResult(s -> {
            Project p = projectService.load(projectId);
            if (outlineService.get(projectId).getChapters().isEmpty()) {
                throw new InvalidStateException("챕터가 없는 Outline은 확정할 수 없습니다.");
            }
            stateMachine.transition(p, OUTLINE_CONFIRMED);
            activity.recordProject(p, ActivityType.OUTLINE_CONFIRMED, "Outline 확정", 0);
        });
    }

    // ───────────── Chapter ─────────────

    private record Prepared(Long projectId, int number, ProjectStatus previousStatus, ChapterContext context) {
    }

    /**
     * 챕터 순차 생성 (기획서 §6.3, §7.2). SSE 이벤트: token → done(ChapterResponse) | error.
     * 이전 챕터가 모두 확정되어야 다음 챕터를 생성할 수 있습니다 (CHAPTER_REVIEW를 건너뛰지 않음).
     */
    public Flux<ServerSentEvent<Object>> generateChapter(Long projectId, int number) {
        Prepared prepared = inTx(s -> prepareChapter(projectId, number));
        AtomicBoolean finished = new AtomicBoolean(false);

        return ai.generateChapter(prepared.context())
                .concatMap(event -> {
                    String type = Objects.requireNonNullElse(event.event(), "");
                    String data = Objects.requireNonNullElse(event.data(), "");
                    return switch (type) {
                        case "token" -> Mono.just(sse("token", Map.of("text", data)));
                        case "done" -> Mono.fromCallable(() -> completeChapter(prepared, data))
                                .subscribeOn(Schedulers.boundedElastic())
                                .doOnSuccess(r -> finished.set(true))
                                .map(r -> sse("done", r));
                        case "error" -> Mono.<ServerSentEvent<Object>>error(
                                new AiServiceException("AI 서비스 오류: " + data, null));
                        default -> Mono.<ServerSentEvent<Object>>empty();
                    };
                })
                .onErrorResume(e -> {
                    log.warn("chapter generation failed: project={}, chapter={}", projectId, number, e);
                    finished.set(true);
                    return Mono.fromRunnable(() -> revert(prepared))
                            .subscribeOn(Schedulers.boundedElastic())
                            .then(Mono.just(sse("error", Map.of("message", userMessage(e)))));
                })
                .doOnCancel(() -> {
                    if (finished.compareAndSet(false, true)) {
                        Schedulers.boundedElastic().schedule(() -> revert(prepared));
                    }
                });
    }

    private Prepared prepareChapter(Long projectId, int number) {
        Project p = projectService.load(projectId);
        stateMachine.requireIn(p, OUTLINE_CONFIRMED, CHAPTER_REVIEW);
        List<OutlineChapter> outlineChapters = outlineService.get(projectId).getChapters();
        int next = chapterService.confirmedCount(projectId) + 1;
        if (number != next) {
            throw new InvalidStateException("지금 생성할 수 있는 챕터는 " + next + "장입니다.");
        }
        if (number > outlineChapters.size()) {
            throw new InvalidStateException("Outline에 " + number + "장이 없습니다.");
        }
        ChapterContext context = chapterContext(p, number, outlineChapters);
        ProjectStatus previousStatus = p.getStatus();
        stateMachine.transition(p, CHAPTER_GENERATING);
        return new Prepared(projectId, number, previousStatus, context);
    }

    private ChapterResponse completeChapter(Prepared prepared, String doneJson) {
        ChapterGenerateResult result = objectMapper.readValue(doneJson, ChapterGenerateResult.class);
        Map<String, Object> contextMap = objectMapper.convertValue(prepared.context(), new TypeReference<>() {
        });
        return inTx(s -> {
            Project p = projectService.load(prepared.projectId());
            ContinuityResult report = result.report() == null ? new ContinuityResult(List.of(), 0) : result.report();
            Chapter chapter = chapterService.saveDraft(p, prepared.number(), result.title(), result.content(),
                    result.summary(), contextMap, report.issues(), report.score());
            stateMachine.transition(p, CHAPTER_REVIEW);
            return chapterService.toResponse(chapter);
        });
    }

    /** 생성 실패·중단 시 생성 전 상태로 되돌립니다. */
    private void revert(Prepared prepared) {
        try {
            tx.executeWithoutResult(s -> {
                Project p = projectService.load(prepared.projectId());
                if (p.getStatus() == CHAPTER_GENERATING) {
                    stateMachine.transition(p, prepared.previousStatus());
                }
            });
        } catch (RuntimeException e) {
            log.error("failed to revert project {} after chapter generation failure", prepared.projectId(), e);
        }
    }

    /**
     * 사용자 승인 (기획서 §7.2 6단계). 마지막 챕터면 COMPLETED.
     * 생성 뒤 고쳤거나 Clone한 원고는 먼저 다시 요약·검증합니다. 다음 챕터 컨텍스트가 실제 본문과 어긋나지 않게 하기 위해서입니다.
     * AI 호출이 실패하면 승인하지 않습니다. 재검증에서 심각한 이슈가 나오면 결과를 저장하고 승인을 멈춰 사용자에게 보여 줍니다.
     */
    public ChapterResponse confirmChapter(Long chapterId) {
        ChapterReviewRequest review = tx.execute(s -> {
            Chapter chapter = chapterService.load(chapterId);
            requireConfirmable(chapter);
            if (!chapter.isNeedsReview()) {
                return null;
            }
            Project p = chapter.getProject();
            ChapterContext context = chapterContext(p, chapter.getNumber(), outlineService.get(p.getId()).getChapters());
            return new ChapterReviewRequest(context, chapter.getTitle(), chapter.getContent());
        });
        if (review != null) {
            ChapterReviewResult result = ai.reviewChapter(review);
            ContinuityResult report = result.report() == null ? new ContinuityResult(List.of(), 0) : result.report();
            tx.executeWithoutResult(s -> chapterService.applyReview(chapterId, result.summary(), report.issues(), report.score()));
            long high = report.issues().stream().filter(i -> "high".equals(i.severity())).count();
            if (high > 0) {
                throw new InvalidStateException("수정본을 다시 검증했더니 심각한 연속성 이슈가 " + high
                        + "건 있습니다. 리포트를 확인한 뒤 다시 승인해 주세요.");
            }
        }
        return inTx(s -> {
            Chapter chapter = chapterService.load(chapterId);
            Project p = chapter.getProject();
            requireConfirmable(chapter);
            if (chapter.isNeedsReview()) {
                throw new InvalidStateException("검증하는 동안 본문이 바뀌었습니다. 다시 승인해 주세요.");
            }
            int confirmed = chapterService.confirmedCount(p.getId());
            chapter.confirm();
            p.touch();
            activity.recordProject(p, ActivityType.CHAPTER_CONFIRMED, chapter.getNumber() + "장 승인", 0);
            if (confirmed + 1 == outlineService.get(p.getId()).getChapters().size()) {
                stateMachine.transition(p, COMPLETED);
            }
            return chapterService.toResponse(chapter);
        });
    }

    private void requireConfirmable(Chapter chapter) {
        Project p = chapter.getProject();
        stateMachine.requireIn(p, CHAPTER_REVIEW);
        if (chapter.getStatus() != ChapterStatus.DRAFT) {
            throw new InvalidStateException("이미 확정된 챕터입니다.");
        }
        if (chapter.getNumber() != chapterService.confirmedCount(p.getId()) + 1) {
            throw new InvalidStateException("앞 챕터부터 순서대로 확정해야 합니다.");
        }
    }

    /** 컨텍스트 패키지: Bible + Outline 해당 장 + 확정된 이전 챕터 요약 (기획서 §6.3) */
    private ChapterContext chapterContext(Project p, int number, List<OutlineChapter> outlineChapters) {
        List<PreviousChapter> previous = chapterService.confirmedBefore(p.getId(), number).stream()
                .map(c -> new PreviousChapter(c.getNumber(), c.getTitle(), c.getSummary()))
                .toList();
        return new ChapterContext(projectInfo(p), bibleInfo(bibleService.get(p.getId())),
                number, outlineChapters.get(number - 1), previous);
    }

    // ───────────── helpers ─────────────

    /** TransactionTemplate.execute는 null을 반환할 수 있으므로 결과가 비면 바로 실패시킵니다. */
    private <T> T inTx(TransactionCallback<T> action) {
        return Objects.requireNonNull(tx.execute(Objects.requireNonNull(action)), "트랜잭션 결과가 비어 있습니다.");
    }

    /** 작업 기록의 변경 글자 수 계산용 */
    private static String bibleText(StoryBible b) {
        return String.valueOf(BibleSnapshot.from(b));
    }

    private static ProjectInfo projectInfo(Project p) {
        return new ProjectInfo(p.getTitle(), p.getGenre(), p.getModel(), p.getDefaultTone());
    }

    private static BibleInfo bibleInfo(StoryBible b) {
        return new BibleInfo(b.getWorldSetting(), b.getTheme(), b.getTone(),
                b.getCharacters().stream()
                        .map(c -> new CharacterInfo(c.getName(), c.getDescription(), c.getPersonality(),
                                c.getRelationships(), c.getCurrentState()))
                        .toList());
    }

    private static ServerSentEvent<Object> sse(String event, Object data) {
        return ServerSentEvent.builder(Objects.requireNonNull(data)).event(Objects.requireNonNull(event)).build();
    }

    private static String userMessage(Throwable e) {
        if (e instanceof InvalidStateException) {
            return e.getMessage();
        }
        return "챕터 생성에 실패했습니다. 잠시 후 다시 시도해 주세요.";
    }
}
