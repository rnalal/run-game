package com.example.rungame.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

//리소스 미존재 404 예외
@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException{

    //@param message : 클라이언트에게 전달할 에러 메시지
    public NotFoundException(String message) {
        super(message);
    }
}
