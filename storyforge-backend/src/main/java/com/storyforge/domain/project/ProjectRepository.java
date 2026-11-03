package com.storyforge.domain.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByOwnerIdOrderByUpdatedAtDesc(Long ownerId);

    boolean existsByIdAndOwnerId(Long id, Long ownerId);

    List<Project> findByStatus(ProjectStatus status);

    long countByOwnerIdAndStatus(Long ownerId, ProjectStatus status);
}
