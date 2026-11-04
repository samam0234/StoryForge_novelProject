package com.storyforge.exception;

/** 권한이 없거나 이용이 정지된 경우 (403) */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
