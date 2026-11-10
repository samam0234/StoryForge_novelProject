package com.storyforge.exception;

public class NotFoundException extends RuntimeException {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public NotFoundException(String message) {
        super(message);
    }
}
