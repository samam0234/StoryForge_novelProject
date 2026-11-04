package com.storyforge.controller;

import com.storyforge.domain.community.CommunityDtos.PageResponse;
import com.storyforge.domain.community.CommunityDtos.UserSummary;
import com.storyforge.domain.moderation.ContentReport;
import com.storyforge.domain.moderation.ModerationDtos.*;
import com.storyforge.service.auth.CurrentUser;
import com.storyforge.service.moderation.ModerationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 신고, 차단, 관리자 처리. 관리자 여부는 서비스에서 확인합니다. */
@RestController
@RequestMapping("/api/v1")
public class ModerationController {

    private final ModerationService moderation;
    private final CurrentUser currentUser;

    public ModerationController(ModerationService moderation, CurrentUser currentUser) {
        this.moderation = moderation;
        this.currentUser = currentUser;
    }

    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportReceipt report(@Valid @RequestBody ReportRequest request) {
        return moderation.report(request, currentUser.id());
    }

    @PutMapping("/users/{userId}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void block(@PathVariable Long userId) {
        moderation.block(currentUser.id(), userId);
    }

    @DeleteMapping("/users/{userId}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unblock(@PathVariable Long userId) {
        moderation.unblock(currentUser.id(), userId);
    }

    @GetMapping("/me/blocks")
    public List<UserSummary> blocks() {
        return moderation.blockedUsers(currentUser.id());
    }

    @GetMapping("/admin/reports")
    public PageResponse<ReportView> reports(@RequestParam(defaultValue = "OPEN") ContentReport.Status status,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return moderation.list(status, page, size, currentUser.id());
    }

    @PutMapping("/admin/reports/{reportId}")
    public ReportView decide(@PathVariable Long reportId, @Valid @RequestBody DecideRequest request) {
        return moderation.decide(reportId, request, currentUser.id());
    }
}
