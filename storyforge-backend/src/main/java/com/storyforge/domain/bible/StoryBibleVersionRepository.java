package com.storyforge.domain.bible;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoryBibleVersionRepository extends JpaRepository<StoryBibleVersion, Long> {

    List<StoryBibleVersion> findByBibleProjectIdOrderByVersionDesc(Long projectId);
}
