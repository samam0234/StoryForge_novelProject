package com.storyforge.exception;

/** 현재 파이프라인 상태에서 허용되지 않는 요청 */
public class InvalidStateException extends RuntimeException {

    public InvalidStateException(String message) {
        super(message);
    }
}
