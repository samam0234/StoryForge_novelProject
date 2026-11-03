package com.storyforge.exception;

/** 로그인 실패, 인증 정보 없음 (401) */
public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException(String message) {
        super(message);
    }
}
