package com.storyforge.controller;

import com.storyforge.domain.chapter.ChapterDtos.ChapterResponse;
import com.storyforge.domain.chapter.ChapterDtos.UpdateChapterRequest;
import com.storyforge.service.chapter.ChapterService;
import com.storyforge.service.orchestration.PipelineOrchestrator;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ChapterController {

    private final ChapterService chapterService;
    private final PipelineOrchestrator orchestrator;

    public ChapterController(ChapterService chapterService, PipelineOrchestrator orchestrator) {
        this.chapterService = chapterService;
        this.orchestrator = orchestrator;
    }

    @GetMapping("/projects/{projectId}/chapters")
    public List<ChapterResponse> list(@PathVariable Long projectId) {
        return chapterService.list(projectId);
    }

    /** 챕터 생성 SSE 중계 (기획서 §7.2). 이벤트: token, done, error */
    @PostMapping(value = "/projects/{projectId}/chapters/{number}/generate",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> generate(@PathVariable Long projectId, @PathVariable int number) {
        return orchestrator.generateChapter(projectId, number);
    }

    @GetMapping("/chapters/{chapterId}")
    public ChapterResponse get(@PathVariable Long chapterId) {
        return chapterService.get(chapterId);
    }

    @PutMapping("/chapters/{chapterId}")
    public ChapterResponse update(@PathVariable Long chapterId, @RequestBody UpdateChapterRequest request) {
        return chapterService.edit(chapterId, request.title(), request.content());
    }

    @PutMapping("/chapters/{chapterId}/confirm")
    public ChapterResponse confirm(@PathVariable Long chapterId) {
        return orchestrator.confirmChapter(chapterId);
    }
}
