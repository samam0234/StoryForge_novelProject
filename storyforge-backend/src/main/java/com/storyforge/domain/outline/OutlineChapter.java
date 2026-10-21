package com.storyforge.domain.outline;

import java.util.List;

/** Outline의 챕터 한 칸 (기획서 §6.2, §10) */
public record OutlineChapter(
        int order,
        String title,
        List<String> beats,
        List<String> characters,
        String goals,
        List<String> continuityPoints) {
}
