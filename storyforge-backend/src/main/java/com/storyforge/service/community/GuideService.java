package com.storyforge.service.community;

import com.storyforge.client.AiDtos.GuideAction;
import com.storyforge.client.AiDtos.GuideChatRequest;
import com.storyforge.client.AiDtos.GuideChatResponse;
import com.storyforge.client.AiDtos.GuideContext;
import com.storyforge.client.AiServiceClient;
import com.storyforge.domain.community.Work;
import com.storyforge.domain.community.WorkRepository;
import com.storyforge.domain.project.Project;
import com.storyforge.domain.project.ProjectRepository;
import com.storyforge.domain.user.AppUserRepository;
import com.storyforge.exception.NotFoundException;
import com.storyforge.service.project.ProjectService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * AI 가이드챗. 명령어와 답변은 FastAPI가 만들고, 여기서는 사용자 상황을 넘기고 외부 연결 주소를 채웁니다.
 * 브라우저가 이동할 주소는 설정값만 쓰고 모델 응답의 주소는 쓰지 않습니다.
 */
@Service
public class GuideService {

    private final AiServiceClient ai;
    private final AppUserRepository users;
    private final ProjectRepository projects;
    private final ProjectService projectService;
    private final WorkRepository works;
    private final TransactionTemplate tx;
    private final String collabUrl;

    public GuideService(AiServiceClient ai, AppUserRepository users, ProjectRepository projects, ProjectService projectService,
                        WorkRepository works, TransactionTemplate tx,
                        @Value("${storyforge.collab.codicomperchat-url}") String collabUrl) {
        this.ai = ai;
        this.users = users;
        this.projects = projects;
        this.projectService = projectService;
        this.works = works;
        this.tx = tx;
        this.collabUrl = collabUrl == null ? "" : collabUrl.strip();
    }

    public GuideChatResponse chat(String message, Long userId) {
        GuideChatResponse response = ai.guideChat(new GuideChatRequest(message, contextFor(userId)));
        return decorate(response);
    }

    /** 웹소켓 가이드도 같은 사용자 상황을 넘깁니다. */
    public GuideContext contextFor(Long userId) {
        return Objects.requireNonNull(tx.execute(s -> context(userId)));
    }

    /** 모델이 준 외부 주소는 쓰지 않고 설정된 Codicomperchat 주소만 남깁니다. */
    public GuideChatResponse decorate(GuideChatResponse response) {
        return resolveCollab(response);
    }

    private GuideContext context(Long userId) {
        String name = users.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다.")).getDisplayName();
        List<Project> mine = projects.findByOwnerIdOrderByUpdatedAtDesc(userId);
        long published = works.countByAuthorIdAndVisibilityAndHiddenAtIsNull(userId, Work.Visibility.PUBLIC);
        if (mine.isEmpty()) {
            return new GuideContext(name, 0, published, null, null, null, null);
        }
        Project latest = mine.get(0);
        return new GuideContext(name, mine.size(), published, latest.getId(), latest.getTitle(), latest.getStatus().name(),
                projectService.status(latest.getId()).nextChapterNumber());
    }

    private GuideChatResponse resolveCollab(GuideChatResponse response) {
        List<GuideAction> actions = new ArrayList<>();
        String reply = response.reply();
        for (GuideAction action : response.actions()) {
            if (!"collab".equals(action.kind())) {
                actions.add(action);
            } else if (collabUrl.startsWith("https://") || collabUrl.startsWith("http://")) {
                actions.add(new GuideAction(action.label(), "external", collabUrl));
            } else {
                reply += "\n\n(Codicomperchat 연결 주소가 아직 설정되지 않아 이동 버튼을 표시하지 않았습니다.)";
            }
        }
        return new GuideChatResponse(reply, actions);
    }
}
