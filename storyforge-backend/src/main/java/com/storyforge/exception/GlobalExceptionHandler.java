package com.storyforge.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail notFound(NotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ProblemDetail authenticationFailed(AuthenticationFailedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(InvalidStateException.class)
    public ProblemDetail invalidState(InvalidStateException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    /** 같은 프로젝트에 동시 요청이 들어와 먼저 처리된 쪽이 상태를 바꾼 경우 */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail concurrentUpdate(OptimisticLockingFailureException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "다른 요청이 먼저 처리되었습니다. 다시 시도해 주세요.");
    }

    @ExceptionHandler(AiServiceException.class)
    public ProblemDetail aiService(AiServiceException e) {
        log.warn("AI service error", e);
        // 내부 주소는 외부 응답에 노출하지 않습니다.
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                "AI 서비스가 응답하지 않습니다. 잠시 후 다시 시도해 주세요.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + ", " + b)
                .orElse("요청 값이 올바르지 않습니다.");
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }
}
