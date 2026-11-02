package com.storyforge.domain.community;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkChapterRepository extends JpaRepository<WorkChapter, Long> {

    List<WorkChapter> findByWorkIdOrderByNumber(Long workId);

    Optional<WorkChapter> findByWorkIdAndNumber(Long workId, int number);
}
