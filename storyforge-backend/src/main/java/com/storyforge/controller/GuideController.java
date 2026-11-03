package com.storyforge.controller;

import com.storyforge.client.AiDtos.GuideChatResponse;
import com.storyforge.service.auth.CurrentUser;
import com.storyforge.service.community.GuideService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** AI 가이드챗. LLM 호출은 FastAPI에서만 합니다. */
@RestController
@RequestMapping("/api/v1/guide")
public class GuideController {

    public record ChatRequest(@NotBlank @Size(max = 2000) String message) {
    }

    private final GuideService guide;
    private final CurrentUser currentUser;

    public GuideController(GuideService guide, CurrentUser currentUser) {
        this.guide = guide;
        this.currentUser = currentUser;
    }

    @PostMapping("/chat")
    public GuideChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return guide.chat(request.message(), currentUser.id());
    }
}
