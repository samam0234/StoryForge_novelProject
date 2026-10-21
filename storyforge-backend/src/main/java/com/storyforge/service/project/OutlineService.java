package com.storyforge.service.project;

import com.storyforge.domain.outline.Outline;
import com.storyforge.domain.outline.OutlineChapter;
import com.storyforge.domain.outline.OutlineRepository;
import com.storyforge.domain.project.Project;
import com.storyforge.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Outline 저장·조회. 기획서 §8.2의 service/ 아래에 outline 패키지가 없어 프로젝트 단위 데이터로 보고
 * service/project에 둡니다. 위치는 확인 후 옮길 수 있습니다.
 */
@Service
@Transactional(readOnly = true)
public class OutlineService {

    private final OutlineRepository outlines;

    public OutlineService(OutlineRepository outlines) {
        this.outlines = outlines;
    }

    public Outline get(Long projectId) {
        return outlines.findByProjectId(projectId)
                .orElseThrow(() -> new NotFoundException("Outline이 아직 없습니다: project " + projectId));
    }

    /** 순서(order)는 목록 순서대로 1부터 다시 매깁니다 (드래그 앤 드롭 재배치, 기획서 §6.2). */
    @Transactional
    public Outline replace(Project project, List<OutlineChapter> chapters) {
        Outline outline = outlines.findByProjectId(project.getId()).orElseGet(() -> new Outline(project));
        List<OutlineChapter> renumbered = IntStream.range(0, chapters.size())
                .mapToObj(i -> {
                    OutlineChapter c = chapters.get(i);
                    return new OutlineChapter(i + 1, c.title(), c.beats(), c.characters(), c.goals(), c.continuityPoints());
                })
                .toList();
        outline.replaceChapters(renumbered);
        return outlines.save(outline);
    }
}
