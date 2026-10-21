package com.storyforge.service.project;

import com.storyforge.domain.chapter.ChapterRepository;
import com.storyforge.domain.chapter.ChapterStatus;
import com.storyforge.domain.outline.OutlineRepository;
import com.storyforge.domain.project.Project;
import com.storyforge.domain.project.ProjectDtos.CreateProjectRequest;
import com.storyforge.domain.project.ProjectDtos.ProjectResponse;
import com.storyforge.domain.project.ProjectDtos.ProjectStatusResponse;
import com.storyforge.domain.project.ProjectRepository;
import com.storyforge.domain.project.ProjectStatus;
import com.storyforge.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projects;
    private final OutlineRepository outlines;
    private final ChapterRepository chapters;

    public ProjectService(ProjectRepository projects, OutlineRepository outlines, ChapterRepository chapters) {
        this.projects = projects;
        this.outlines = outlines;
        this.chapters = chapters;
    }

    @Transactional
    public ProjectResponse create(CreateProjectRequest request) {
        Project project = projects.save(new Project(request.title().trim(), request.genre()));
        return toResponse(project);
    }

    public List<ProjectResponse> list() {
        return projects.findAllByOrderByUpdatedAtDesc().stream().map(this::toResponse).toList();
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
    public void delete(Long id) {
        projects.delete(load(id));
    }

    public Project load(Long id) {
        return projects.findById(id).orElseThrow(() -> new NotFoundException("프로젝트를 찾을 수 없습니다: " + id));
    }

    private int totalChapters(Long projectId) {
        return outlines.findByProjectId(projectId).map(o -> o.getChapters().size()).orElse(0);
    }

    private ProjectResponse toResponse(Project p) {
        return new ProjectResponse(p.getId(), p.getTitle(), p.getGenre(), p.getStatus(),
                totalChapters(p.getId()), chapters.countByProjectIdAndStatus(p.getId(), ChapterStatus.CONFIRMED),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
