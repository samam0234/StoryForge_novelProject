package com.storyforge.domain.moderation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContentReportRepository extends JpaRepository<ContentReport, Long> {

    List<ContentReport> findByStatusOrderByCreatedAtDesc(ContentReport.Status status);

    Optional<ContentReport> findByReporterIdAndTargetTypeAndTargetIdAndStatus(
            Long reporterId, ContentReport.TargetType targetType, Long targetId, ContentReport.Status status);
}
