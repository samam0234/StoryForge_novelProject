package com.storyforge.service.orchestration;

import com.storyforge.domain.project.Project;
import com.storyforge.domain.project.ProjectStatus;
import com.storyforge.exception.InvalidStateException;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.storyforge.domain.project.ProjectStatus.*;

/**
 * 프로젝트 파이프라인 상태 머신. 프로젝트 상태 전이는 이 클래스를 통해서만 일어납니다.
 * 기준: 기획서 v1.2 §7.2, docs/Architecture/pipeline-state-machine.md
 */
@Component
public class PipelineStateMachine {

    /** 기획서 §7.2에 정의된 전이 */
    private static final Map<ProjectStatus, Set<ProjectStatus>> DEFINED = new EnumMap<>(ProjectStatus.class);

    /**
     * 임시 전이 — 기획서에 정의가 없어 구현에 필요한 최소한만 둡니다. 사용자 확인 후 확정하거나 제거합니다.
     * - CHAPTER_REVIEW → CHAPTER_GENERATING : 다음 챕터 생성, 검토 중 재생성
     * - CHAPTER_GENERATING → OUTLINE_CONFIRMED : 첫 챕터 생성 실패·중단 시 복귀
     */
    private static final Map<ProjectStatus, Set<ProjectStatus>> PROVISIONAL = new EnumMap<>(ProjectStatus.class);

    static {
        DEFINED.put(BIBLE_DRAFT, EnumSet.of(BIBLE_CONFIRMED));
        DEFINED.put(BIBLE_CONFIRMED, EnumSet.of(OUTLINE_DRAFT));
        DEFINED.put(OUTLINE_DRAFT, EnumSet.of(OUTLINE_CONFIRMED));
        DEFINED.put(OUTLINE_CONFIRMED, EnumSet.of(CHAPTER_GENERATING));
        DEFINED.put(CHAPTER_GENERATING, EnumSet.of(CHAPTER_REVIEW));
        DEFINED.put(CHAPTER_REVIEW, EnumSet.of(COMPLETED));

        PROVISIONAL.put(CHAPTER_REVIEW, EnumSet.of(CHAPTER_GENERATING));
        PROVISIONAL.put(CHAPTER_GENERATING, EnumSet.of(OUTLINE_CONFIRMED));
    }

    public boolean canTransition(ProjectStatus from, ProjectStatus to) {
        return DEFINED.getOrDefault(from, Set.of()).contains(to)
                || PROVISIONAL.getOrDefault(from, Set.of()).contains(to);
    }

    public void transition(Project project, ProjectStatus to) {
        ProjectStatus from = project.getStatus();
        if (!canTransition(from, to)) {
            throw new InvalidStateException("상태를 " + from + "에서 " + to + "(으)로 바꿀 수 없습니다.");
        }
        project.changeStatus(to);
    }

    public void requireIn(Project project, ProjectStatus... allowed) {
        if (Arrays.stream(allowed).noneMatch(s -> s == project.getStatus())) {
            throw new InvalidStateException("현재 상태(" + project.getStatus() + ")에서는 할 수 없는 작업입니다. 필요한 상태: "
                    + Arrays.toString(allowed));
        }
    }
}
