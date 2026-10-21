package com.storyforge.domain.outline;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OutlineRepository extends JpaRepository<Outline, Long> {

    Optional<Outline> findByProjectId(Long projectId);
}
