package com.storyforge.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** ADMIN_EMAILS에 있는 주소만 관리자가 됩니다. 요청으로 역할을 바꾸지 않습니다. */
@Component
public class AdminDirectory {

    private final Set<String> emails;

    public AdminDirectory(@Value("${storyforge.admin.emails:}") String raw) {
        this.emails = Arrays.stream(raw.split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean contains(String email) {
        return email != null && emails.contains(email);
    }
}
