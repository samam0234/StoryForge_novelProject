package com.storyforge.exception;

/** FastAPI AI Service 장애·타임아웃 (기획서 §15: 명확한 에러 전파) */
public class AiServiceException extends RuntimeException {

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
