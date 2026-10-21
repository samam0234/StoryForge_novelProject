package com.storyforge.domain.chapter;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    List<Chapter> findByProjectIdOrderByNumber(Long projectId);

    Optional<Chapter> findByProjectIdAndNumber(Long projectId, int number);

    int countByProjectIdAndStatus(Long projectId, ChapterStatus status);
}
