package com.storyforge.controller;

import com.storyforge.domain.bible.BibleDtos.BibleResponse;
import com.storyforge.domain.bible.BibleDtos.GenerateBibleRequest;
import com.storyforge.domain.bible.BibleDtos.UpdateBibleRequest;
import com.storyforge.service.bible.BibleService;
import com.storyforge.service.orchestration.PipelineOrchestrator;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/bible")
public class BibleController {

    private final BibleService bibleService;
    private final PipelineOrchestrator orchestrator;

    public BibleController(BibleService bibleService, PipelineOrchestrator orchestrator) {
        this.bibleService = bibleService;
        this.orchestrator = orchestrator;
    }

    @GetMapping
    public BibleResponse get(@PathVariable Long projectId) {
        return BibleResponse.from(bibleService.get(projectId));
    }

    @PutMapping
    public BibleResponse update(@PathVariable Long projectId, @Valid @RequestBody UpdateBibleRequest request) {
        return orchestrator.updateBible(projectId, request);
    }

    @PostMapping("/generate")
    public BibleResponse generate(@PathVariable Long projectId, @Valid @RequestBody GenerateBibleRequest request) {
        return orchestrator.generateBible(projectId, request.prompt());
    }

    /** BIBLE_DRAFT → BIBLE_CONFIRMED (기획서 §11 목록 밖 보조 엔드포인트) */
    @PutMapping("/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirm(@PathVariable Long projectId) {
        orchestrator.confirmBible(projectId);
    }
}
