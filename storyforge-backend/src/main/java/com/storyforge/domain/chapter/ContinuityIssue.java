package com.storyforge.domain.chapter;

/** 기획서 §10: issues[] (type, description, severity) */
public record ContinuityIssue(String type, String description, String severity,
                              String evidence, String reference, String suggestion) {
    public ContinuityIssue(String type, String description, String severity) {
        this(type, description, severity, null, null, null);
    }
}
