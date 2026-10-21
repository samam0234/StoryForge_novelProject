package com.storyforge.service.chapter;

import com.storyforge.domain.chapter.*;
import com.storyforge.domain.chapter.ChapterDtos.ChapterResponse;
import com.storyforge.domain.project.Project;
import com.storyforge.exception.InvalidStateException;
import com.storyforge.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ChapterService {

    private final ChapterRepository chapters;
    private final ContinuityReportRepository reports;

    public ChapterService(ChapterRepository chapters, ContinuityReportRepository reports) {
        this.chapters = chapters;
        this.reports = reports;
    }

    public List<ChapterResponse> list(Long projectId) {
        return chapters.findByProjectIdOrderByNumber(projectId).stream().map(this::toResponse).toList();
    }

    public List<Chapter> confirmedBefore(Long projectId, int number) {
        return chapters.findByProjectIdOrderByNumber(projectId).stream()
                .filter(c -> c.getNumber() < number && c.getStatus() == ChapterStatus.CONFIRMED)
                .toList();
    }

    public int confirmedCount(Long projectId) {
        return chapters.countByProjectIdAndStatus(projectId, ChapterStatus.CONFIRMED);
    }

    public Chapter load(Long chapterId) {
        return chapters.findById(chapterId)
                .orElseThrow(() -> new NotFoundException("챕터를 찾을 수 없습니다: " + chapterId));
    }

    public ChapterResponse get(Long chapterId) {
        return toResponse(load(chapterId));
    }

    /** 생성 결과를 Draft로 저장하고 Continuity 리포트를 함께 남깁니다. */
    @Transactional
    public Chapter saveDraft(Project project, int number, String title, String content, String summary,
                             Map<String, Object> generationContext, List<ContinuityIssue> issues, int score) {
        Chapter chapter = chapters.findByProjectIdAndNumber(project.getId(), number)
                .orElseGet(() -> new Chapter(project, number));
        if (chapter.getStatus() == ChapterStatus.CONFIRMED) {
            throw new InvalidStateException("이미 확정된 챕터는 다시 생성할 수 없습니다: " + number);
        }
        chapter.applyDraft(title, content, summary, generationContext);
        chapter = chapters.save(chapter);
        reports.save(new ContinuityReport(chapter, issues, score));
        return chapter;
    }

    @Transactional
    public ChapterResponse edit(Long chapterId, String title, String content) {
        Chapter chapter = load(chapterId);
        if (chapter.getStatus() == ChapterStatus.CONFIRMED) {
            throw new InvalidStateException("확정된 챕터는 수정할 수 없습니다.");
        }
        chapter.edit(title, content);
        return toResponse(chapter);
    }

    public ChapterResponse toResponse(Chapter c) {
        return ChapterResponse.from(c, reports.findFirstByChapterIdOrderByCreatedAtDesc(c.getId()).orElse(null));
    }
}
