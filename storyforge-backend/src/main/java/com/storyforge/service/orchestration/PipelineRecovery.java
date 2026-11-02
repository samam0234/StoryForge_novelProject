package com.storyforge.service.orchestration;

import com.storyforge.domain.chapter.ChapterRepository;
import com.storyforge.domain.project.Project;
import com.storyforge.domain.project.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.storyforge.domain.project.ProjectStatus.*;

/**
 * 생성 도중 서버가 종료되면 프로젝트가 CHAPTER_GENERATING에 남습니다. 시작할 때 생성 전 상태로 되돌립니다.
 * 서버 한 대 기준입니다. 여러 대로 늘리면 다른 서버의 진행 중 생성까지 되돌리므로 재검토해야 합니다.
 */
@Component
public class PipelineRecovery implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PipelineRecovery.class);

    private final ProjectRepository projects;
    private final ChapterRepository chapters;
    private final PipelineStateMachine stateMachine;

    public PipelineRecovery(ProjectRepository projects, ChapterRepository chapters, PipelineStateMachine stateMachine) {
        this.projects = projects;
        this.chapters = chapters;
        this.stateMachine = stateMachine;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        recoverStuckProjects();
    }

    /** 챕터가 하나라도 있으면 CHAPTER_REVIEW, 없으면 첫 챕터 전이므로 OUTLINE_CONFIRMED로 돌아갑니다. */
    @Transactional
    public int recoverStuckProjects() {
        List<Project> stuck = projects.findByStatus(CHAPTER_GENERATING);
        for (Project p : stuck) {
            stateMachine.transition(p, chapters.existsByProjectId(p.getId()) ? CHAPTER_REVIEW : OUTLINE_CONFIRMED);
            log.warn("생성 중 종료된 프로젝트 복구: project={}, status={}", p.getId(), p.getStatus());
        }
        return stuck.size();
    }
}
