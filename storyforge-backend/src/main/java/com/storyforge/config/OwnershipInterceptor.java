package com.storyforge.config;

import com.storyforge.domain.chapter.ChapterRepository;
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
 * 경로 변수 {projectId}, {chapterId}를 검사합니다. 주인과 초대를 수락한 공동 창작자만 통과하고,
 * 그 외에는 404입니다. 삭제·설정·게시·초대처럼 주인만 하는 일은 각 서비스가 거절합니다.
 */
@Component
public class OwnershipInterceptor implements HandlerInterceptor {

    private final ChapterRepository chapters;
    private final CurrentUser currentUser;
    private final ProjectAccess access;

    public OwnershipInterceptor(ChapterRepository chapters, CurrentUser currentUser, ProjectAccess access) {
        this.chapters = chapters;
        this.currentUser = currentUser;
        this.access = access;
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
        if (projectId != null) {
            if (!access.canRead(parse(projectId), currentUser.id())) {
                throw new NotFoundException("프로젝트를 찾을 수 없습니다: " + projectId);
            }
        }
        String chapterId = vars.get("chapterId");
        if (chapterId != null) {
            long id = parse(chapterId);
            Long ownerProject = chapters.findProjectIdById(id)
                    .orElseThrow(() -> new NotFoundException("챕터를 찾을 수 없습니다: " + chapterId));
            if (!access.canRead(ownerProject, currentUser.id())) {
                throw new NotFoundException("챕터를 찾을 수 없습니다: " + chapterId);
            }
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
