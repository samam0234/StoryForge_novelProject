package com.storyforge.config;

import com.storyforge.domain.chapter.ChapterRepository;
import com.storyforge.domain.project.ProjectRepository;
import com.storyforge.exception.NotFoundException;
import com.storyforge.service.auth.CurrentUser;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

/**
 * 경로 변수 {projectId}, {chapterId}의 소유자를 일괄 검사합니다. 다른 사용자의 리소스는 404입니다.
 * 새 엔드포인트도 이 변수 이름을 쓰면 자동으로 검사됩니다.
 */
@Component
public class OwnershipInterceptor implements HandlerInterceptor {

    private final ProjectRepository projects;
    private final ChapterRepository chapters;
    private final CurrentUser currentUser;

    public OwnershipInterceptor(ProjectRepository projects, ChapterRepository chapters, CurrentUser currentUser) {
        this.projects = projects;
        this.chapters = chapters;
        this.currentUser = currentUser;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (request.getDispatcherType() != DispatcherType.REQUEST) {
            return true;
        }
        @SuppressWarnings("unchecked")
        Map<String, String> vars = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (vars == null) {
            return true;
        }
        String projectId = vars.get("projectId");
        if (projectId != null && !projects.existsByIdAndOwnerId(parse(projectId), currentUser.id())) {
            throw new NotFoundException("프로젝트를 찾을 수 없습니다: " + projectId);
        }
        String chapterId = vars.get("chapterId");
        if (chapterId != null && !chapters.existsByIdAndProjectOwnerId(parse(chapterId), currentUser.id())) {
            throw new NotFoundException("챕터를 찾을 수 없습니다: " + chapterId);
        }
        return true;
    }

    private static Long parse(String id) {
        try {
            return Long.valueOf(id);
        } catch (NumberFormatException e) {
            throw new NotFoundException("잘못된 식별자입니다: " + id);
        }
    }
}
