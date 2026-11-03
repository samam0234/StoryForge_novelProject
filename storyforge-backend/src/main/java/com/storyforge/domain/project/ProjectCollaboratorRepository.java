package com.storyforge.domain.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectCollaboratorRepository extends JpaRepository<ProjectCollaborator, Long> {

    Optional<ProjectCollaborator> findByProjectIdAndUserId(Long projectId, Long userId);

    boolean existsByProjectIdAndUserIdAndStatus(Long projectId, Long userId, ProjectCollaborator.Status status);

    List<ProjectCollaborator> findByProjectIdOrderByCreatedAtAsc(Long projectId);

    List<ProjectCollaborator> findByUserIdOrderByCreatedAtDesc(Long userId);
}
