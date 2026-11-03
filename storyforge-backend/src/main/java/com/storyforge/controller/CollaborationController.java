package com.storyforge.controller;

import com.storyforge.domain.project.CollabDtos.CollaboratorView;
import com.storyforge.domain.project.CollabDtos.CommitRequest;
import com.storyforge.domain.project.CollabDtos.CommitView;
import com.storyforge.domain.project.CollabDtos.InviteRequest;
import com.storyforge.service.auth.CurrentUser;
import com.storyforge.service.project.CollaborationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 함께 창작. 프로젝트 경로는 OwnershipInterceptor가 참여자를 확인합니다. */
@RestController
@RequestMapping("/api/v1")
public class CollaborationController {

    private final CollaborationService collaboration;
    private final CurrentUser currentUser;

    public CollaborationController(CollaborationService collaboration, CurrentUser currentUser) {
        this.collaboration = collaboration;
        this.currentUser = currentUser;
    }

    @GetMapping("/projects/{projectId}/collaborators")
    public List<CollaboratorView> members(@PathVariable Long projectId) {
        return collaboration.listMembers(projectId);
    }

    @PostMapping("/projects/{projectId}/collaborators")
    @ResponseStatus(HttpStatus.CREATED)
    public CollaboratorView invite(@PathVariable Long projectId, @Valid @RequestBody InviteRequest request) {
        return collaboration.invite(projectId, currentUser.id(), request.email());
    }

    @DeleteMapping("/projects/{projectId}/collaborators/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long projectId, @PathVariable Long userId) {
        collaboration.remove(projectId, currentUser.id(), userId);
    }

    @GetMapping("/projects/{projectId}/commits")
    public List<CommitView> commits(@PathVariable Long projectId) {
        return collaboration.listCommits(projectId);
    }

    @GetMapping("/projects/{projectId}/commits/{commitId}")
    public CommitView commit(@PathVariable Long projectId, @PathVariable Long commitId) {
        return collaboration.getCommit(projectId, commitId);
    }

    @PostMapping("/projects/{projectId}/commits")
    @ResponseStatus(HttpStatus.CREATED)
    public CommitView addCommit(@PathVariable Long projectId, @Valid @RequestBody CommitRequest request) {
        return collaboration.commit(projectId, currentUser.id(), request);
    }

    @GetMapping("/me/invitations")
    public List<CollaboratorView> invitations() {
        return collaboration.myInvitations(currentUser.id());
    }

    @PostMapping("/me/invitations/{invitationId}/accept")
    public CollaboratorView accept(@PathVariable Long invitationId) {
        return collaboration.accept(invitationId, currentUser.id());
    }

    @DeleteMapping("/me/invitations/{invitationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decline(@PathVariable Long invitationId) {
        collaboration.decline(invitationId, currentUser.id());
    }
}
