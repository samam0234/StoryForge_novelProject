package com.storyforge.domain.moderation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ContentReportRepository extends JpaRepository<ContentReport, Long> {

    @Query(value = "SELECT id FROM moderation_guard WHERE id = 1 FOR UPDATE", nativeQuery = true)
    int lockOperations();

    @EntityGraph(attributePaths = "reporter")
    Page<ContentReport> findByStatusOrderByCreatedAtDescIdDesc(ContentReport.Status status, Pageable pageable);

    Optional<ContentReport> findFirstByReporterIdAndTargetTypeAndTargetIdAndStatus(
            Long reporterId, ContentReport.TargetType targetType, Long targetId, ContentReport.Status status);

    @EntityGraph(attributePaths = "reporter")
    List<ContentReport> findByTargetTypeAndTargetIdAndStatus(ContentReport.TargetType targetType, Long targetId,
                                                             ContentReport.Status status);

    long countByTargetTypeAndTargetIdAndStatus(ContentReport.TargetType targetType, Long targetId, ContentReport.Status status);
}
