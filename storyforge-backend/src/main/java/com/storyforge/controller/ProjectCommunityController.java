package com.storyforge.controller;

import com.storyforge.domain.activity.ActivityDtos.WorkGraph;
import com.storyforge.domain.community.CommunityDtos.PublishRequest;
import com.storyforge.domain.community.CommunityDtos.UpstreamCompare;
import com.storyforge.domain.community.CommunityDtos.WorkCard;
import com.storyforge.service.auth.CurrentUser;
import com.storyforge.service.community.ActivityService;
import com.storyforge.service.community.CloneService;
import com.storyforge.service.community.WorkService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** 내 프로젝트의 게시, 작업 기록, 원본 비교. 소유자 검사는 OwnershipInterceptor가 합니다. */
@RestController
@RequestMapping("/api/v1/projects/{projectId}")
public class ProjectCommunityController {

    private final WorkService workService;
    private final CloneService cloneService;
    private final ActivityService activity;
    private final CurrentUser currentUser;

    public ProjectCommunityController(WorkService workService, CloneService cloneService, ActivityService activity,
                                      CurrentUser currentUser) {
        this.workService = workService;
        this.cloneService = cloneService;
        this.activity = activity;
        this.currentUser = currentUser;
    }

    @PostMapping("/publish")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkCard publish(@PathVariable Long projectId, @Valid @RequestBody PublishRequest request) {
        return workService.publish(projectId, request, currentUser.id());
    }

    @GetMapping("/activity")
    public WorkGraph activity(@PathVariable Long projectId) {
        return activity.projectGraph(projectId);
    }

    @GetMapping("/upstream")
    public UpstreamCompare upstream(@PathVariable Long projectId) {
        return cloneService.compare(projectId, currentUser.id());
    }
}
