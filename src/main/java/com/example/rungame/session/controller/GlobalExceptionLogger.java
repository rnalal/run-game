package com.example.rungame.session.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

//전역 예외 로깅 전용 핸들러
@Slf4j
@RestControllerAdvice
public class GlobalExceptionLogger {

    //처리되지 않은 모든 예외를 한 번 더 잡아서 로그로 남기는 핸들러
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> logException(Exception e) {
        log.error("UNHANDLED SERVER ERROR", e);
        return ResponseEntity.status(500).body("server error");
    }
}
