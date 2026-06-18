package com.example.rungame.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

//페이지 요청 전용 보안 예외 처리기
@ControllerAdvice(annotations = Controller.class)
public class PageSecurityExceptionHandler {

    //인증/인가 관련 예외 처리
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
