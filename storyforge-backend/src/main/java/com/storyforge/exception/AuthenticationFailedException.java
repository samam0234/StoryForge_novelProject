package com.storyforge.exception;

/** 로그인 실패, 인증 정보 없음 (401) */
public class AuthenticationFailedException extends RuntimeException {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public AuthenticationFailedException(String message) {
        super(message);
    }
}
