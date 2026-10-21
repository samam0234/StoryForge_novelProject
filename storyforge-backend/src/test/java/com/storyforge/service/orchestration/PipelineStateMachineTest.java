package com.storyforge.service.orchestration;

import com.storyforge.domain.project.Project;
import com.storyforge.exception.InvalidStateException;
import org.junit.jupiter.api.Test;

import static com.storyforge.domain.project.ProjectStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PipelineStateMachineTest {

    private final PipelineStateMachine sm = new PipelineStateMachine();

    @Test
    void followsDefinedOrderFromBibleDraftToCompleted() {
        Project p = new Project("t", "g");
        for (var next : new com.storyforge.domain.project.ProjectStatus[]{
                BIBLE_CONFIRMED, OUTLINE_DRAFT, OUTLINE_CONFIRMED, CHAPTER_GENERATING, CHAPTER_REVIEW, COMPLETED}) {
            sm.transition(p, next);
        }
        assertThat(p.getStatus()).isEqualTo(COMPLETED);
    }

    @Test
    void rejectsSkippingStages() {
        Project p = new Project("t", "g");
        assertThatThrownBy(() -> sm.transition(p, OUTLINE_DRAFT)).isInstanceOf(InvalidStateException.class);
        assertThat(sm.canTransition(OUTLINE_CONFIRMED, CHAPTER_REVIEW)).isFalse();
        assertThat(sm.canTransition(CHAPTER_GENERATING, COMPLETED)).isFalse();
    }

    @Test
    void allowsProvisionalChapterLoopAndRevert() {
        assertThat(sm.canTransition(CHAPTER_REVIEW, CHAPTER_GENERATING)).isTrue();
        assertThat(sm.canTransition(CHAPTER_GENERATING, OUTLINE_CONFIRMED)).isTrue();
        assertThat(sm.canTransition(COMPLETED, CHAPTER_GENERATING)).isFalse();
    }
}
