package com.example.rungame.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/*
* 페이지 요청 전용 보안 예외 처리기
*
* - 컨트롤러 기반 페이지 요청에서 발생한 인증/인가 예외를 처리
* - REST API(RestController)용 GlobalExceptionHandler와 역할 분리
*
* 목적:
* - API 요청 -> JSON 에러 응답
* - 페이지 요청 -> 로그인 페이지 redirect
* */
@ControllerAdvice(annotations = Controller.class)
public class PageSecurityExceptionHandler {

    /*
    * 인증/인가 관련 예외 처리
    * - 로그인하지 않은 사용자 접근
    * - 권한이 없는 페이지 접근
    *
    * 처리 방식:
    * - JSON 응답
    * - 로그인 페이지로 redirect
    * */
    @ExceptionHandler({
            AccessDeniedException.class,
            AuthenticationCredentialsNotFoundException.class
    })
    public String handleSecurityException(HttpServletRequest request,
                                          HttpServletResponse response) {

        //로그인 페이지로 이동 + 에러 파라미터 전달
        return "redirect:/login?error=need_login";
    }
}
