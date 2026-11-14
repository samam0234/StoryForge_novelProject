package com.storyforge.service.orchestration;

import com.storyforge.client.AiDtos.BibleInfo;
import com.storyforge.client.AiDtos.CharacterInfo;
import com.storyforge.client.AiDtos.ProjectInfo;
import com.storyforge.config.ProjectAccess;
import com.storyforge.domain.bible.StoryBible;
import com.storyforge.domain.chapter.Chapter;
import com.storyforge.domain.outline.OutlineChapter;
import com.storyforge.domain.outline.OutlineRepository;
import com.storyforge.domain.project.Project;
import com.storyforge.domain.project.ProjectRepository;
import com.storyforge.exception.NotFoundException;
import com.storyforge.service.bible.BibleService;
import com.storyforge.service.chapter.ChapterService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 로그인한 사용자의 프로젝트로 다음 장면 개척 요청을 만듭니다.
 * 전송은 브라우저 세션의 AI 웹소켓이 맡고, 상태 머신은 옮기지 않습니다.
 */
@Service
public class StoryScoutService {

    private final ProjectRepository projects;
    private final ProjectAccess access;
    private final BibleService bibles;
    private final OutlineRepository outlines;
    private final ChapterService chapters;
    private final ObjectMapper json;

    public StoryScoutService(ProjectRepository projects, ProjectAccess access, BibleService bibles,
                             OutlineRepository outlines, ChapterService chapters, ObjectMapper json) {
        this.projects = projects;
        this.access = access;
        this.bibles = bibles;
        this.outlines = outlines;
        this.chapters = chapters;
        this.json = json;
    }

    public String payload(Long userId, Long projectId, String messageId) {
        Project project = projects.findById(projectId)
                .orElseThrow(() -> new NotFoundException("프로젝트를 찾을 수 없습니다."));
        if (!access.canRead(project.getId(), userId)) {
            throw new NotFoundException("프로젝트를 찾을 수 없습니다.");
        }
        StoryBible bible = bibles.find(projectId)
                .orElseThrow(() -> new NotFoundException("Story Bible을 만든 뒤에 다음 장면을 열 수 있습니다."));
        List<OutlineChapter> outline = outlines.findByProjectId(projectId)
                .map(item -> item.getChapters() == null ? List.<OutlineChapter>of() : item.getChapters())
                .orElse(List.of());
        List<Chapter> confirmed = chapters.confirmedBefore(projectId, Integer.MAX_VALUE);
        Chapter last = confirmed.isEmpty() ? null : confirmed.get(confirmed.size() - 1);
        int next = confirmed.size() + 1;
        OutlineChapter current = outline.stream().filter(item -> item.order() == next).findFirst()
                .orElse(outline.isEmpty() ? null : outline.get(outline.size() - 1));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("op", "scout");
        body.put("id", messageId);
        body.put("project", new ProjectInfo(project.getTitle(), project.getGenre(), project.getModel(),
                project.getDefaultTone(), project.getForm().name(), project.getCreativeBrief()));
        List<com.storyforge.domain.bible.Character> people = bible.getCharacters() == null ? List.of() : bible.getCharacters();
        body.put("bible", new BibleInfo(bible.getWorldSetting(), bible.getTheme(), bible.getTone(),
                people.stream()
                        .map(c -> new CharacterInfo(c.getName(), c.getDescription(), c.getPersonality(),
                                c.getRelationships(), c.getCurrentState()))
                        .toList()));
        body.put("outlineChapter", current == null ? null : normalized(current));
        body.put("previousEnding", last == null ? null : ending(last.getContent()));
        body.put("previousSummary", last == null ? null : last.getSummary());
        try {
            return json.writeValueAsString(body);
        } catch (Exception e) {
            throw new IllegalStateException("장면 개척 요청을 만들지 못했습니다.", e);
        }
    }

    private static OutlineChapter normalized(OutlineChapter chapter) {
        return new OutlineChapter(chapter.order(), chapter.title(),
                chapter.beats() == null ? List.of() : chapter.beats(),
                chapter.characters() == null ? List.of() : chapter.characters(),
                chapter.goals(),
                chapter.continuityPoints() == null ? List.of() : chapter.continuityPoints());
    }

    private static String ending(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        int count = content.codePointCount(0, content.length());
        return content.substring(content.offsetByCodePoints(0, Math.max(0, count - 1200)));
    }
}
