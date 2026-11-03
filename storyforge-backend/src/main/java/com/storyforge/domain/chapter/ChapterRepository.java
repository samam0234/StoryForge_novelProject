package com.storyforge.domain.chapter;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    List<Chapter> findByProjectIdOrderByNumber(Long projectId);

    Optional<Chapter> findByProjectIdAndNumber(Long projectId, int number);

    int countByProjectIdAndStatus(Long projectId, ChapterStatus status);

    boolean existsByIdAndProjectOwnerId(Long id, Long ownerId);

    @Query("select c.project.id from Chapter c where c.id = :id")
    Optional<Long> findProjectIdById(@Param("id") Long id);

    boolean existsByProjectId(Long projectId);
}
