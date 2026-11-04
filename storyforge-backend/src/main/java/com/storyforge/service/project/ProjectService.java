package com.storyforge.service.project;

import com.storyforge.config.ProjectAccess;
import com.storyforge.domain.activity.ActivityEventRepository;
import com.storyforge.domain.activity.ActivityType;
import com.storyforge.domain.chapter.ChapterRepository;
import com.storyforge.domain.community.WorkRepository;
import com.storyforge.domain.chapter.ChapterStatus;
import com.storyforge.domain.outline.OutlineRepository;
import com.storyforge.domain.project.Project;
import com.storyforge.domain.project.ProjectCollaborator;
import com.storyforge.domain.project.ProjectCollaboratorRepository;
import com.storyforge.domain.project.ProjectDtos.CreateProjectRequest;
import com.storyforge.domain.project.ProjectDtos.ProjectResponse;
import com.storyforge.domain.project.ProjectDtos.ProjectStatusResponse;
import com.storyforge.domain.project.ProjectDtos.UpdateProjectRequest;
import com.storyforge.domain.project.ProjectRepository;
import com.storyforge.domain.project.ProjectStatus;
import com.storyforge.exception.InvalidStateException;
import com.storyforge.exception.NotFoundException;
import com.storyforge.service.auth.CurrentUser;
import com.storyforge.service.community.ActivityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projects;
    private final OutlineRepository outlines;
    private final ChapterRepository chapters;
    private final WorkRepository works;
    private final ActivityEventRepository events;
    private final ActivityService activity;
    private final ProjectCollaboratorRepository collaborators;
    private final CurrentUser currentUser;
    private final ProjectAccess access;

    public ProjectService(ProjectRepository projects, OutlineRepository outlines, ChapterRepository chapters,
                          WorkRepository works, ActivityEventRepository events, ActivityService activity,
                          ProjectCollaboratorRepository collaborators, CurrentUser currentUser, ProjectAccess access) {
        this.projects = projects;
        this.outlines = outlines;
        this.chapters = chapters;
        this.works = works;
        this.events = events;
        this.activity = activity;
        this.collaborators = collaborators;
        this.currentUser = currentUser;
        this.access = access;
    }

    @Transactional
    public ProjectResponse create(CreateProjectRequest request, Long ownerId) {
        Project project = projects.save(new Project(request.title().trim(), request.genre(), ownerId, request.form()));
        activity.recordProject(project, ActivityType.PROJECT_CREATED, "프로젝트 시작", 0);
        return toResponse(project, "OWNER");
    }

    public List<ProjectResponse> list(Long ownerId) {
        List<Project> mine = new ArrayList<>(projects.findByOwnerIdOrderByUpdatedAtDesc(ownerId));
        List<Long> sharedIds = collaborators.findByUserIdOrderByCreatedAtDesc(ownerId).stream()
                .filter(c -> c.getStatus() == ProjectCollaborator.Status.ACCEPTED)
                .map(item -> item.getProjectId())
                .toList();
        if (!sharedIds.isEmpty()) {
            mine.addAll(projects.findAllById(sharedIds));
        }
        mine.sort((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()));
        Map<Long, long[]> chars = new HashMap<>();
        if (!mine.isEmpty()) {
            for (ActivityEventRepository.ProjectChars c : events.charsByProject(mine.stream().map(item -> item.getId()).toList())) {
                chars.computeIfAbsent(c.getProjectId(), k -> new long[2])[c.getActor() == ActivityType.Actor.AGENT ? 1 : 0] = c.getChars();
            }
        }
        return mine.stream().map(p -> {
            long[] c = chars.getOrDefault(p.getId(), new long[2]);
            String role = access.isOwner(p.getId(), ownerId) ? "OWNER" : "COLLABORATOR";
            return toResponse(p, c[0], c[1], role);
        }).toList();
    }

    public ProjectResponse get(Long id, Long viewerId) {
        Project project = load(id);
        String role = access.isOwner(id, viewerId) ? "OWNER" : "COLLABORATOR";
        return toResponse(project, role);
    }

    public ProjectStatusResponse status(Long id) {
        Project project = load(id);
        int total = totalChapters(id);
        int confirmed = chapters.countByProjectIdAndStatus(id, ChapterStatus.CONFIRMED);
        boolean chapterPhase = project.getStatus() == ProjectStatus.OUTLINE_CONFIRMED
                || project.getStatus() == ProjectStatus.CHAPTER_REVIEW;
        Integer next = chapterPhase && confirmed < total ? confirmed + 1 : null;
        return new ProjectStatusResponse(id, project.getStatus(), total, confirmed, next);
    }

    @Transactional
    public ProjectResponse update(Long id, UpdateProjectRequest request) {
        Project project = load(id);
        requireOwner(project);
        project.updateSettings(request.title().trim(), request.genre(), request.model(), request.defaultTone(),
                request.exportFormat());
        return toResponse(project, "OWNER");
    }

    @Transactional
    public void delete(Long id) {
        Project project = load(id);
        requireOwner(project);
        projects.delete(Objects.requireNonNull(project));
    }

    public Project load(Long id) {
        return projects.findById(Objects.requireNonNull(id)).orElseThrow(() -> new NotFoundException("프로젝트를 찾을 수 없습니다: " + id));
    }

    private int totalChapters(Long projectId) {
        return outlines.findByProjectId(projectId).map(o -> o.getChapters().size()).orElse(0);
    }

    private void requireOwner(Project project) {
        if (!access.isOwner(project.getId(), currentUser.id())) {
            throw new InvalidStateException("이 작업은 작품 주인만 할 수 있습니다.");
        }
    }

    private ProjectResponse toResponse(Project p, String role) {
        long manual = 0;
        long agent = 0;
        for (ActivityEventRepository.ProjectChars c : events.charsByProject(List.of(p.getId()))) {
            if (c.getActor() == ActivityType.Actor.AGENT) {
                agent = c.getChars();
            } else {
                manual = c.getChars();
            }
        }
        return toResponse(p, manual, agent, role);
    }

    private ProjectResponse toResponse(Project p, long manualChars, long agentChars, String role) {
        return new ProjectResponse(p.getId(), p.getTitle(), p.getGenre(), p.getStatus(),
                p.getModel(), p.getDefaultTone(), p.getExportFormat(),
                totalChapters(p.getId()), chapters.countByProjectIdAndStatus(p.getId(), ChapterStatus.CONFIRMED),
                p.getForm(), works.findIdByProjectId(p.getId()).orElse(null), p.getClonedFromWorkId(),
                p.getClonedFromTitle(), p.getCloneMode(), manualChars, agentChars,
                p.getCreatedAt(), p.getUpdatedAt(), role);
    }
}
