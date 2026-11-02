package com.storyforge.service.bible;

import com.storyforge.domain.bible.BibleDtos.BibleSnapshot;
import com.storyforge.domain.bible.BibleDtos.BibleVersionResponse;
import com.storyforge.domain.bible.Character;
import com.storyforge.domain.bible.StoryBible;
import com.storyforge.domain.bible.StoryBibleRepository;
import com.storyforge.domain.bible.StoryBibleVersion;
import com.storyforge.domain.bible.StoryBibleVersionRepository;
import com.storyforge.domain.project.Project;
import com.storyforge.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class BibleService {

    private final StoryBibleRepository bibles;
    private final StoryBibleVersionRepository versions;

    public BibleService(StoryBibleRepository bibles, StoryBibleVersionRepository versions) {
        this.bibles = bibles;
        this.versions = versions;
    }

    public Optional<StoryBible> find(Long projectId) {
        return bibles.findByProjectId(projectId);
    }

    public StoryBible get(Long projectId) {
        return find(projectId)
                .orElseThrow(() -> new NotFoundException("Story Bible이 아직 없습니다: project " + projectId));
    }

    public List<BibleVersionResponse> versions(Long projectId) {
        return versions.findByBibleProjectIdOrderByVersionDesc(projectId).stream()
                .map(v -> new BibleVersionResponse(v.getVersion(), v.getCreatedAt(), v.getSnapshot()))
                .toList();
    }

    /** 생성·편집 모두 전체 교체 후 버전을 올립니다 (기획서 §6.1 버전 관리). */
    @Transactional
    public StoryBible replace(Project project, String worldSetting, String theme, String tone,
                              List<Character> characters) {
        StoryBible bible = find(project.getId()).orElseGet(() -> new StoryBible(project));
        bible.replace(worldSetting, theme, tone, characters);
        project.touch();
        StoryBible saved = Objects.requireNonNull(bibles.save(bible));
        versions.save(new StoryBibleVersion(saved, BibleSnapshot.from(saved)));
        return saved;
    }
}
