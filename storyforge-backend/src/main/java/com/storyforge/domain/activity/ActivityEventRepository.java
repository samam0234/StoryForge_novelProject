package com.storyforge.domain.activity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface ActivityEventRepository extends JpaRepository<ActivityEvent, Long> {

    interface DayCount {
        String getDay();

        long getCnt();
    }

    interface ActorTotal {
        ActivityType.Actor getActor();

        long getEvents();

        long getChars();
    }

    List<ActivityEvent> findTop2000ByWorkIdAndTypeInOrderByCreatedAtDesc(Long workId, Collection<ActivityType> types);

    List<ActivityEvent> findTop2000ByProjectIdAndTypeInOrderByCreatedAtDesc(Long projectId, Collection<ActivityType> types);

    @Query("""
            select e.actor as actor, count(e) as events, coalesce(sum(e.changedChars), 0) as chars
            from ActivityEvent e where e.work.id = :workId and e.type in :types group by e.actor
            """)
    List<ActorTotal> totalsByWork(@Param("workId") Long workId, @Param("types") Collection<ActivityType> types);

    @Query("""
            select e.actor as actor, count(e) as events, coalesce(sum(e.changedChars), 0) as chars
            from ActivityEvent e where e.projectId = :projectId and e.type in :types group by e.actor
            """)
    List<ActorTotal> totalsByProject(@Param("projectId") Long projectId, @Param("types") Collection<ActivityType> types);

    /** 게시 전 작업 기록을 작품에 잇습니다. 이미 다른 작품을 가리키는 기록(Clone)은 건드리지 않습니다. */
    @Modifying
    @Query(value = "UPDATE activity_event SET work_id = :workId WHERE project_id = :projectId AND work_id IS NULL", nativeQuery = true)
    void attachToWork(@Param("projectId") Long projectId, @Param("workId") Long workId);

    @Query(value = """
            SELECT to_char(created_at AT TIME ZONE :zone, 'YYYY-MM-DD') AS day, count(*) AS cnt
            FROM activity_event
            WHERE user_id = :userId AND created_at >= :from AND created_at < :to
            GROUP BY 1 ORDER BY 1
            """, nativeQuery = true)
    List<DayCount> dailyCounts(@Param("userId") Long userId, @Param("zone") String zone,
                               @Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            SELECT to_char(created_at AT TIME ZONE :zone, 'YYYY-MM-DD') AS day, count(*) AS cnt
            FROM activity_event WHERE user_id = :userId GROUP BY 1 ORDER BY 1
            """, nativeQuery = true)
    List<DayCount> allDays(@Param("userId") Long userId, @Param("zone") String zone);

    /** 프로필 활동 내역. 본인이 아니면 공개 작품·게시글에 연결된 기록만 봅니다. */
    @Query(value = """
            select e from ActivityEvent e left join fetch e.work w left join fetch e.post p
            where e.userId = :userId and e.createdAt >= :from and e.createdAt < :to
              and (:self = true or (p.id is not null and p.hiddenAt is null)
                   or (w.visibility = com.storyforge.domain.community.Work$Visibility.PUBLIC and w.hiddenAt is null))
            order by e.createdAt desc, e.id desc
            """,
            countQuery = """
            select count(e) from ActivityEvent e left join e.work w left join e.post p
            where e.userId = :userId and e.createdAt >= :from and e.createdAt < :to
              and (:self = true or (p.id is not null and p.hiddenAt is null)
                   or (w.visibility = com.storyforge.domain.community.Work$Visibility.PUBLIC and w.hiddenAt is null))
            """)
    Page<ActivityEvent> feed(@Param("userId") Long userId, @Param("self") boolean self, @Param("from") Instant from,
                             @Param("to") Instant to, Pageable pageable);

    long countByUserIdAndType(Long userId, ActivityType type);

    @Query("select coalesce(sum(e.changedChars), 0) from ActivityEvent e where e.userId = :userId and e.actor = :actor")
    long sumChars(@Param("userId") Long userId, @Param("actor") ActivityType.Actor actor);

    @Query("""
            select e.projectId as projectId, e.actor as actor, coalesce(sum(e.changedChars), 0) as chars
            from ActivityEvent e where e.projectId in :projectIds group by e.projectId, e.actor
            """)
    List<ProjectChars> charsByProject(@Param("projectIds") Collection<Long> projectIds);

    interface ProjectChars {
        Long getProjectId();

        ActivityType.Actor getActor();

        long getChars();
    }
}
