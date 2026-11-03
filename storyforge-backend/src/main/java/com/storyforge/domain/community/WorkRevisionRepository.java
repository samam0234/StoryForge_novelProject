package com.storyforge.domain.community;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkRevisionRepository extends JpaRepository<WorkRevision, Long> {

    List<WorkRevision> findByWorkIdOrderByRevisionDesc(Long workId);
}
