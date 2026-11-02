package com.storyforge.domain.community;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkCloneRepository extends JpaRepository<WorkClone, Long> {

    @EntityGraph(attributePaths = "user")
    List<WorkClone> findTop50ByWorkIdOrderByCreatedAtDesc(Long workId);

    long countByWorkIdAndMode(Long workId, WorkClone.Mode mode);
}
