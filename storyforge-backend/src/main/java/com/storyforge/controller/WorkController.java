package com.storyforge.controller;

import com.storyforge.domain.activity.ActivityDtos.WorkGraph;
import com.storyforge.domain.community.CommunityDtos.*;
import com.storyforge.domain.community.Work;
import com.storyforge.service.auth.CurrentUser;
import com.storyforge.service.community.*;
import com.storyforge.service.project.ExportService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** 게시 작품: 게시판, 읽기, 수정, 추천, 댓글, Clone, 공유. 읽기는 로그인 없이도 됩니다 (SecurityConfig). */
@RestController
@RequestMapping("/api/v1")
public class WorkController {

    private final WorkService workService;
    private final CloneService cloneService;
    private final ShareService shareService;
    private final EngagementService engagement;
    private final ActivityService activity;
    private final CurrentUser currentUser;

    public WorkController(WorkService workService, CloneService cloneService, ShareService shareService,
                          EngagementService engagement, ActivityService activity, CurrentUser currentUser) {
        this.workService = workService;
        this.cloneService = cloneService;
        this.shareService = shareService;
        this.engagement = engagement;
        this.activity = activity;
        this.currentUser = currentUser;
    }

    @GetMapping("/works")
    public PageResponse<WorkCard> board(@RequestParam Work.Kind kind, @RequestParam(required = false) String genre,
                                        @RequestParam(required = false) String q,
                                        @RequestParam(required = false) String sort,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return workService.board(kind, genre, q, sort, page, size);
    }

    @GetMapping("/works/{workId}")
    public WorkDetail detail(@PathVariable Long workId) {
        return workService.detail(workId, currentUser.idOrNull());
    }

    @GetMapping("/works/{workId}/chapters/{number}")
    public WorkChapterResponse chapter(@PathVariable Long workId, @PathVariable int number) {
        return workService.chapter(workId, number, currentUser.idOrNull());
    }

    @PutMapping("/works/{workId}")
    public WorkCard update(@PathVariable Long workId, @Valid @RequestBody UpdateWorkRequest request) {
        return workService.update(workId, request, currentUser.id());
    }

    @PutMapping("/works/{workId}/chapters/{number}")
    public WorkChapterResponse editChapter(@PathVariable Long workId, @PathVariable int number,
                                           @Valid @RequestBody UpdateWorkChapterRequest request) {
        return workService.editChapter(workId, number, request, currentUser.id());
    }

    /** 프로젝트에서 승인된 챕터를 다시 가져옵니다. */
    @PostMapping("/works/{workId}/sync")
    public WorkCard sync(@PathVariable Long workId, @Valid @RequestBody SyncWorkRequest request) {
        return workService.sync(workId, request, currentUser.id());
    }

    @DeleteMapping("/works/{workId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long workId) {
        workService.delete(workId, currentUser.id());
    }

    /** 내려받기는 로그인이 필요합니다. 작가가 허용하지 않으면 409입니다. */
    @GetMapping("/works/{workId}/export")
    public ResponseEntity<byte[]> export(@PathVariable Long workId, @RequestParam String format) {
        ExportService.ExportFile file = workService.export(workId, format, currentUser.id());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.filename(), StandardCharsets.UTF_8).build().toString())
                .body(file.bytes());
    }

    /** 작업 그래프: 수동·에이전트 비율, 날짜별 막대, 기록 표 */
    @GetMapping("/works/{workId}/activity")
    public WorkGraph activity(@PathVariable Long workId) {
        workService.loadVisible(workId, currentUser.idOrNull());
        return activity.workGraph(workId);
    }

    // ───────────── 추천 ─────────────

    @PutMapping("/works/{workId}/like")
    public LikeResponse like(@PathVariable Long workId) {
        return engagement.likeWork(workId, currentUser.id());
    }

    @DeleteMapping("/works/{workId}/like")
    public LikeResponse unlike(@PathVariable Long workId) {
        return engagement.unlikeWork(workId, currentUser.id());
    }

    // ───────────── 댓글 ─────────────

    @GetMapping("/works/{workId}/comments")
    public List<CommentView> comments(@PathVariable Long workId) {
        return engagement.workComments(workId, currentUser.idOrNull());
    }

    @PostMapping("/works/{workId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentView comment(@PathVariable Long workId, @Valid @RequestBody CommentRequest request) {
        return engagement.commentOnWork(workId, request, currentUser.id());
    }

    @PutMapping("/comments/{commentId}")
    public CommentView editComment(@PathVariable Long commentId, @Valid @RequestBody CommentEditRequest request) {
        return engagement.editComment(commentId, request, currentUser.id());
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long commentId) {
        engagement.deleteComment(commentId, currentUser.id());
    }

    // ───────────── Clone ─────────────

    @PostMapping("/works/{workId}/clone")
    @ResponseStatus(HttpStatus.CREATED)
    public CloneResponse cloneWork(@PathVariable Long workId, @Valid @RequestBody CloneRequest request) {
        return cloneService.cloneWork(workId, request, currentUser.id());
    }

    @GetMapping("/works/{workId}/network")
    public CloneNetwork network(@PathVariable Long workId) {
        return cloneService.network(workId, currentUser.idOrNull());
    }

    // ───────────── 공유 ─────────────

    @PostMapping("/works/{workId}/shares")
    @ResponseStatus(HttpStatus.CREATED)
    public ShareCreated share(@PathVariable Long workId, @Valid @RequestBody ShareRequest request) {
        return shareService.share(workId, request, currentUser.id());
    }

    @GetMapping("/works/{workId}/shares")
    public ShareGraph shares(@PathVariable Long workId) {
        return shareService.graph(workId, currentUser.idOrNull());
    }

    @PostMapping("/shares/{token}/visit")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void visit(@PathVariable String token) {
        shareService.visit(token);
    }
}
