package com.example.rungame.common.exception;

import com.example.rungame.qna.exception.QnaAccessDeniedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

//전역 예외 처리 핸들러
@RestControllerAdvice
public class GlobalExceptionHandler {

    //@Valid 검증 실패 예외 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req){
        BindingResult br = ex.getBindingResult();

        //필드별 검증 에러 추출
        var fields = br.getFieldErrors().stream()
                .map(f -> ApiError.FieldError.builder().field(f.getField()).reason(f.getDefaultMessage()).build())
                .collect(Collectors.toList());

        var err = ApiError.builder()
                .code(ErrorCode.INVALID_INPUT.getCode())
                .message(ErrorCode.INVALID_INPUT.getMessage())
                .status(ErrorCode.INVALID_INPUT.getStatus().value())
                .path(req.getRequestURI())
                .timestamp(Instant.now())
                .errors(fields)
                .build();
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus()).body(err);
    }

    //잘못된 요청(400) 계열 예외 처리
    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiError> handleBadRequest(Exception ex, HttpServletRequest req) {
        var code = ErrorCode.INVALID_INPUT;
        var err = ApiError.builder()
                .code(code.getCode())
                .message(ex.getMessage() != null ? ex.getMessage() : code.getMessage())
                .status(code.getStatus().value())
                .path(req.getRequestURI())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(code.getStatus()).body(err);
    }

    //처리되지 않은 모든 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAny(Exception ex, HttpServletRequest req) {

        //Spring Security 인증 예외는 그대로 던져서
        //SecurityFilterChain에서 처리하도록 위임
        if (ex instanceof org.springframework.security.core.AuthenticationException ae) {
            throw ae;
        }

        if (ex instanceof org.springframework.security.access.AccessDeniedException ade) {
            throw ade;
        }

        var code = ErrorCode.INTERNAL_ERROR;
        var err = ApiError.builder()
                .code(code.getCode())
                .message(code.getMessage())
                .status(code.getStatus().value())
                .path(req.getRequestURI())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(code.getStatus()).body(err);
    }

    //QnA 도메인 전용 접근 권한 예외 처리
    @ExceptionHandler(QnaAccessDeniedException.class)
    public ResponseEntity<ApiError> handleQnaAccessDenied(
            QnaAccessDeniedException ex,
            HttpServletRequest req
    ) {
        int status = ex.getCode().equals("LOGIN_REQUIRED") ? 401 : 403;

        return ResponseEntity.status(status).body(
                ApiError.builder()
                        .code(ex.getCode())
                        .message(ex.getMessage())
                        .status(status)
                        .path(req.getRequestURI())
                        .timestamp(Instant.now())
                        .build()
        );
    }
}
