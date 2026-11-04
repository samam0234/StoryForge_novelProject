package com.storyforge.domain.community;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IssueEventRepository extends JpaRepository<IssueEvent, Long> {

    @Query("""
            select e from IssueEvent e join fetch e.work w join fetch w.author
            where w.visibility = com.storyforge.domain.community.Work$Visibility.PUBLIC
              and w.hiddenAt is null
            order by e.createdAt desc, e.id desc
            limit 30
            """)
    List<IssueEvent> recent();

    /** 같은 작품·지표·단위는 한 번만 남깁니다. */
    @Modifying
    @Query(value = """
            INSERT INTO issue_event (work_id, metric, threshold, created_at)
            VALUES (:workId, :metric, :threshold, now())
            ON CONFLICT (work_id, metric, threshold) DO NOTHING
            """, nativeQuery = true)
    int reach(@Param("workId") Long workId, @Param("metric") String metric, @Param("threshold") int threshold);
}
