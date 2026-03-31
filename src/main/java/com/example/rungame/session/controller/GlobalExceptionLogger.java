package com.example.rungame.session.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/*
* 전역 예외 로깅 전용 핸들러
*
* - 컨트롤러 계층에서 처리되지 않고 올라온 예외를 한 곳에서 받아서 서버 로그에 남기고
*   클라이언트에는 공통 에러 응답을 내려주는 역할
*
* - @RestControllerAdvice
*   : 모든 @RestController에 대해 전역적으로 적용되는 예외 처리기
* - @ExceptionHandler(Exception.class)
*   : 아직 개별 컨트롤러,핸들러에서 처리하지 않은 예외들을 마지막에 한 번 더 잡음
* */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionLogger {

    /*
    * 처리되지 않은 모든 예외를 한 번 더 잡아서 로그로 남기는 핸들러
    *
    * @param e : 컨트롤러 계층에서 던져진 예외
    * @return : HTTP 500 + 단순 문자열 응답
    *
    * - UNHANDLED SERVER ERROR라는 태그와 함께 전체 스택트레이스를 error 레벨로 기록
    * */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> logException(Exception e) {
        log.error("UNHANDLED SERVER ERROR", e);
        return ResponseEntity.status(500).body("server error");
    }
}
