package com.storyforge.domain.moderation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    List<UserBlock> findByBlockerIdOrderByCreatedAtDesc(Long blockerId);

    @Query("select b.blockedId from UserBlock b where b.blockerId = :blockerId")
    List<Long> blockedIds(@Param("blockerId") Long blockerId);

    @Modifying
    @Query(value = "INSERT INTO user_block (blocker_id, blocked_id, created_at) VALUES (:blocker, :blocked, now()) ON CONFLICT DO NOTHING", nativeQuery = true)
    int block(@Param("blocker") Long blocker, @Param("blocked") Long blocked);

    @Modifying
    @Query("delete from UserBlock b where b.blockerId = :blocker and b.blockedId = :blocked")
    int unblock(@Param("blocker") Long blocker, @Param("blocked") Long blocked);
}
