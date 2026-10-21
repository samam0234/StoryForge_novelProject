package com.storyforge.domain.chapter;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContinuityReportRepository extends JpaRepository<ContinuityReport, Long> {

    Optional<ContinuityReport> findFirstByChapterIdOrderByCreatedAtDesc(Long chapterId);
}
