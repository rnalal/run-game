package com.example.rungame.qna.exception;

import lombok.Getter;

//QnA 접근 권한 예외
@Getter
public class QnaAccessDeniedException extends RuntimeException {

    //도메인 전용 에러 코드
    private final String code;

    //생성자
    public QnaAccessDeniedException(String code, String message) {
        super(message);
        this.code = code;
    }
}
