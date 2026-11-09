package com.storyforge.service.community;

import com.storyforge.exception.InvalidStateException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** 운영자가 넣은 표현이 글에 있으면 거절합니다. 목록이 비어 있으면 검사하지 않습니다. */
@Component
public class ContentPolicy {

    private final List<String> terms;

    public ContentPolicy(@Value("${storyforge.moderation.blocklist:}") String raw) {
        this.terms = Arrays.stream(raw.split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public void check(String... texts) {
        if (terms.isEmpty()) {
            return;
        }
        for (String text : texts) {
            if (text == null || text.isBlank()) {
                continue;
            }
            String lower = text.toLowerCase(Locale.ROOT);
            for (String term : terms) {
                if (lower.contains(term)) {
                    throw new InvalidStateException("사용할 수 없는 표현이 있습니다.");
                }
            }
        }
    }
}
