package com.storyforge.service.project;

import com.storyforge.domain.activity.ActivityEventRepository;
import com.storyforge.domain.activity.ActivityType;
import com.storyforge.domain.chapter.ChapterRepository;
import com.storyforge.domain.community.WorkRepository;
import com.storyforge.domain.chapter.ChapterStatus;
import com.storyforge.domain.outline.OutlineRepository;
import com.storyforge.domain.project.Project;
import com.storyforge.domain.project.ProjectDtos.CreateProjectRequest;
import com.storyforge.domain.project.ProjectDtos.ProjectResponse;
import com.storyforge.domain.project.ProjectDtos.ProjectStatusResponse;
import com.storyforge.domain.project.ProjectDtos.UpdateProjectRequest;
import com.storyforge.domain.project.ProjectRepository;
import com.storyforge.domain.project.ProjectStatus;
import com.storyforge.exception.NotFoundException;
import com.storyforge.service.community.ActivityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public ProjectService(ProjectRepository projects, OutlineRepository outlines, ChapterRepository chapters,
                          WorkRepository works, ActivityEventRepository events, ActivityService activity) {
        this.projects = projects;
        this.outlines = outlines;
        this.chapters = chapters;
        this.works = works;
        this.events = events;
        this.activity = activity;
    }

    @Transactional
    public ProjectResponse create(CreateProjectRequest request, Long ownerId) {
        Project project = projects.save(new Project(request.title().trim(), request.genre(), ownerId, request.form()));
        activity.recordProject(project, ActivityType.PROJECT_CREATED, "프로젝트 시작", 0);
        return toResponse(project);
    }

    public List<ProjectResponse> list(Long ownerId) {
        List<Project> mine = projects.findByOwnerIdOrderByUpdatedAtDesc(ownerId);
        // 프로젝트 목록의 수동·에이전트 비율 막대
        Map<Long, long[]> chars = new HashMap<>();
        if (!mine.isEmpty()) {
            for (ActivityEventRepository.ProjectChars c : events.charsByProject(mine.stream().map(p -> p.getId()).toList())) {
                chars.computeIfAbsent(c.getProjectId(), k -> new long[2])[c.getActor() == ActivityType.Actor.AGENT ? 1 : 0] = c.getChars();
            }
        }
        return mine.stream().map(p -> {
            long[] c = chars.getOrDefault(p.getId(), new long[2]);
            return toResponse(p, c[0], c[1]);
        }).toList();
    }

    public ProjectResponse get(Long id) {
        return toResponse(load(id));
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
        project.updateSettings(request.title().trim(), request.genre(), request.model(), request.defaultTone(),
                request.exportFormat());
        return toResponse(project);
    }

    @Transactional
    public void delete(Long id) {
        projects.delete(Objects.requireNonNull(load(id)));
    }

    public Project load(Long id) {
        return projects.findById(Objects.requireNonNull(id)).orElseThrow(() -> new NotFoundException("프로젝트를 찾을 수 없습니다: " + id));
    }

    private int totalChapters(Long projectId) {
        return outlines.findByProjectId(projectId).map(o -> o.getChapters().size()).orElse(0);
    }

    private ProjectResponse toResponse(Project p) {
        long manual = 0;
        long agent = 0;
        for (ActivityEventRepository.ProjectChars c : events.charsByProject(List.of(p.getId()))) {
            if (c.getActor() == ActivityType.Actor.AGENT) {
                agent = c.getChars();
            } else {
                manual = c.getChars();
            }
        }
        return toResponse(p, manual, agent);
    }

    private ProjectResponse toResponse(Project p, long manualChars, long agentChars) {
        return new ProjectResponse(p.getId(), p.getTitle(), p.getGenre(), p.getStatus(),
                p.getModel(), p.getDefaultTone(), p.getExportFormat(),
                totalChapters(p.getId()), chapters.countByProjectIdAndStatus(p.getId(), ChapterStatus.CONFIRMED),
                p.getForm(), works.findIdByProjectId(p.getId()).orElse(null), p.getClonedFromWorkId(),
                p.getClonedFromTitle(), p.getCloneMode(), manualChars, agentChars,
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
