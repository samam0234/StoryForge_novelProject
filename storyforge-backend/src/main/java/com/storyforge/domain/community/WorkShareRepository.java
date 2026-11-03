package com.storyforge.domain.community;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkShareRepository extends JpaRepository<WorkShare, Long> {

    @EntityGraph(attributePaths = "user")
    List<WorkShare> findTop500ByWorkIdOrderByCreatedAtDesc(Long workId);

    @Modifying
    @Query("update WorkShare s set s.visitCount = s.visitCount + 1 where s.token = :token")
    int addVisit(@Param("token") String token);
}
