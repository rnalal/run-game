package com.example.rungame.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/*
* 리소스 미존재 404 예외
* - 요청한 엔티티/자원이 존재하지 않을 때 사용
* - @ResponseStatus 를 통해
*   별도의 예외 처리 없이도 HTTP 404 반환 가능
*
* GlobalExceptionHandler와 함께 사용할 경우
* 도메인 단에서 의도를 명확히 표현하는 용도
* */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException{

    //@param message : 클라이언트에게 전달할 에러 메시지
    public NotFoundException(String message) {
        super(message);
    }
}
