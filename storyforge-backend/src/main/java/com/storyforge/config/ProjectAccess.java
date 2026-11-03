package com.storyforge.config;

import com.storyforge.domain.project.ProjectCollaborator;
import com.storyforge.domain.project.ProjectCollaboratorRepository;
import com.storyforge.domain.project.ProjectRepository;
import org.springframework.stereotype.Component;

/** 프로젝트 읽기 권한. 주인과 초대를 수락한 공동 창작자만 원고를 봅니다. */
@Component
public class ProjectAccess {

    private final ProjectRepository projects;
    private final ProjectCollaboratorRepository collaborators;

    public ProjectAccess(ProjectRepository projects, ProjectCollaboratorRepository collaborators) {
        this.projects = projects;
        this.collaborators = collaborators;
    }

    public boolean isOwner(Long projectId, Long userId) {
        return userId != null && projects.existsByIdAndOwnerId(projectId, userId);
    }

    public boolean canRead(Long projectId, Long userId) {
        return isOwner(projectId, userId)
                || (userId != null && collaborators.existsByProjectIdAndUserIdAndStatus(
                projectId, userId, ProjectCollaborator.Status.ACCEPTED));
    }
}
