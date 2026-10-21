package com.storyforge.domain.bible;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoryBibleRepository extends JpaRepository<StoryBible, Long> {

    @EntityGraph(attributePaths = "characters")
    Optional<StoryBible> findByProjectId(Long projectId);
}
