package com.storyforge.domain.chapter;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChapterVersionRepository extends JpaRepository<ChapterVersion, Long> {

    List<ChapterVersion> findByChapterIdOrderByVersionDesc(Long chapterId);

    int countByChapterId(Long chapterId);
}
