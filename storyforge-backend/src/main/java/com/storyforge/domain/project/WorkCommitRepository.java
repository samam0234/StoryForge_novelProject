package com.storyforge.domain.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkCommitRepository extends JpaRepository<WorkCommit, Long> {

    List<WorkCommit> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    Optional<WorkCommit> findFirstByProjectIdOrderByCreatedAtDesc(Long projectId);

    Optional<WorkCommit> findByIdAndProjectId(Long id, Long projectId);
}
