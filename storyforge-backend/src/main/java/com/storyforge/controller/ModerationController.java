package com.storyforge.controller;

import com.storyforge.domain.moderation.ModerationDtos.BlockRequest;
import com.storyforge.domain.moderation.ModerationDtos.LlmNotice;
import com.storyforge.domain.moderation.ModerationDtos.ReportRequest;
import com.storyforge.domain.moderation.ModerationDtos.ReportView;
import com.storyforge.service.auth.CurrentUser;
import com.storyforge.service.community.LlmDisclosure;
import com.storyforge.service.community.ModerationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 신고, 차단, 관리자 처리, 외부 언어 모델 안내 */
@RestController
@RequestMapping("/api/v1")
public class ModerationController {

    private final ModerationService moderation;
    private final LlmDisclosure llm;
    private final CurrentUser currentUser;

    public ModerationController(ModerationService moderation, LlmDisclosure llm, CurrentUser currentUser) {
        this.moderation = moderation;
        this.llm = llm;
        this.currentUser = currentUser;
    }

    @GetMapping("/llm-notice")
    public LlmNotice llmNotice() {
        return llm.notice();
    }

    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportView report(@Valid @RequestBody ReportRequest request) {
        return moderation.report(request, currentUser.id());
    }

    @PostMapping("/blocks")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void block(@Valid @RequestBody BlockRequest request) {
        moderation.block(request, currentUser.id());
    }

    @DeleteMapping("/blocks/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unblock(@PathVariable Long userId) {
        moderation.unblock(userId, currentUser.id());
    }

    @GetMapping("/admin/reports")
    public List<ReportView> reports() {
        return moderation.openReports(currentUser.id());
    }

    @PostMapping("/admin/reports/{reportId}/dismiss")
    public ReportView dismiss(@PathVariable Long reportId) {
        return moderation.dismiss(reportId, currentUser.id());
    }

    @PostMapping("/admin/reports/{reportId}/hide")
    public ReportView hide(@PathVariable Long reportId) {
        return moderation.hide(reportId, currentUser.id());
    }
}
