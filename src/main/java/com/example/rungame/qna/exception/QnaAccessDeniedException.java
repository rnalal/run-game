package com.example.rungame.qna.exception;

import lombok.Getter;

/*
* QnA 접근 권한 예외
* - QnA 질문,답변에 대해 사용자가 접근 권한이 없을 때보다 구체적인 에러 코드를 함께
*   던지기 위한 커스텀 예외
* */
@Getter
public class QnaAccessDeniedException extends RuntimeException {

    /*
    * 도메인 전용 에러 코드
    * - GolbalExceptionHandler에서 HTTP 상태,응답 메세지를 구성할 때 사용하거나
    *   프런트에서 상황별 UI 처리를 분기할 때 활용할 수 있음
    * */
    private final String code;

    /*
    * 생성자
    *
    * @param code : 도메인 에러 코드
    * @param message : 사용자, 로그용 메세지
    * */
    public QnaAccessDeniedException(String code, String message) {
        super(message);
        this.code = code;
    }
}
