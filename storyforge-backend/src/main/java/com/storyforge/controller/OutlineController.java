package com.storyforge.controller;

import com.storyforge.domain.outline.OutlineDtos.GenerateOutlineRequest;
import com.storyforge.domain.outline.OutlineDtos.OutlineResponse;
import com.storyforge.domain.outline.OutlineDtos.UpdateOutlineRequest;
import com.storyforge.service.orchestration.PipelineOrchestrator;
import com.storyforge.service.project.OutlineService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/outline")
public class OutlineController {

    private final OutlineService outlineService;
    private final PipelineOrchestrator orchestrator;

    public OutlineController(OutlineService outlineService, PipelineOrchestrator orchestrator) {
        this.outlineService = outlineService;
        this.orchestrator = orchestrator;
    }

    @GetMapping
    public OutlineResponse get(@PathVariable Long projectId) {
        return OutlineResponse.from(outlineService.get(projectId));
    }

    @PutMapping
    public OutlineResponse update(@PathVariable Long projectId, @Valid @RequestBody UpdateOutlineRequest request) {
        return orchestrator.updateOutline(projectId, request.chapters());
    }

    @PostMapping("/generate")
    public OutlineResponse generate(@PathVariable Long projectId, @Valid @RequestBody GenerateOutlineRequest request) {
        return orchestrator.generateOutline(projectId, request.chapterCount());
    }

    /** OUTLINE_DRAFT → OUTLINE_CONFIRMED (기획서 §11 목록 밖 보조 엔드포인트) */
    @PutMapping("/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirm(@PathVariable Long projectId) {
        orchestrator.confirmOutline(projectId);
    }
}
