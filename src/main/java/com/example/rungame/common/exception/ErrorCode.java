package com.example.rungame.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/*
* 공통 에러 코드 정의 Enum
*
* - 애플리케이션 전반에서 사용하는 표준 에러 코드 집합
* - HTTP 상태 코드 + 내부 에러 코드 + 기본 메시지를 함께 관리
*
* 예외 처리 시 문자열을 직접 사용하지 않고
* ErrorCode 기준으로 일관된 에러 응답을 생성하기 위한 목적
* */
@Getter
public enum ErrorCode {
    /*
    * 잘못된 입력 값
    * - @Valid 검증 실패
    * - 파라미터 형식 오류
    * */
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "c001", "Invalid input"),
    //이메일 중복
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U001", "Email already in use"),
    //닉네임 중복
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "U002", "Nickname alredy in use"),
    //리소스를 찾을 수 없음 -> 존재하지 않는 엔티티 접근
    NOT_FOUND(HttpStatus.NOT_FOUND, "C404", "Resource not found"),
    /*
    * 서버 내부 오류
    * - 처리되지 않은 예외
    * - 예상하지 못한 시스템 오류
    * */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S500", "Internal server error");

    //HTTP 상태 코드
    private final HttpStatus status;
    //내부 에러 코드 - 프론트엔드/로그/모니터링에서 사용
    private final String code;
    //기본 에러 메시지 - 사용자 또는 클라이언트에 전달
    private final String message;

    ErrorCode(HttpStatus status, String code, String message){
        this.status = status; this.code = code; this.message = message;
    }
}
