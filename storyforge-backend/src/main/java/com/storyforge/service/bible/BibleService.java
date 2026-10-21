package com.storyforge.service.bible;

import com.storyforge.domain.bible.Character;
import com.storyforge.domain.bible.StoryBible;
import com.storyforge.domain.bible.StoryBibleRepository;
import com.storyforge.domain.project.Project;
import com.storyforge.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class BibleService {

    private final StoryBibleRepository bibles;

    public BibleService(StoryBibleRepository bibles) {
        this.bibles = bibles;
    }

    public Optional<StoryBible> find(Long projectId) {
        return bibles.findByProjectId(projectId);
    }

    public StoryBible get(Long projectId) {
        return find(projectId)
                .orElseThrow(() -> new NotFoundException("Story Bible이 아직 없습니다: project " + projectId));
    }

    /** 생성·편집 모두 전체 교체 후 버전을 올립니다 (기획서 §6.1 버전 관리). */
    @Transactional
    public StoryBible replace(Project project, String worldSetting, String theme, String tone,
                              List<Character> characters) {
        StoryBible bible = find(project.getId()).orElseGet(() -> new StoryBible(project));
        bible.replace(worldSetting, theme, tone, characters);
        project.touch();
        return bibles.save(bible);
    }
}
