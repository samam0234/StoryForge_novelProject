package com.storyforge.domain.moderation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserBlockRepository extends JpaRepository<UserBlock, UserBlockKey> {

    @Query("select b.id.blockedId from UserBlock b where b.id.blockerId = :userId")
    List<Long> findBlockedIds(@Param("userId") Long userId);

    @Query("select b.id.blockerId from UserBlock b where b.id.blockedId = :userId")
    List<Long> findBlockerIds(@Param("userId") Long userId);
}
