package com.example.rungame.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

//공통 에러 코드 정의 Enum
@Getter
public enum ErrorCode {
    //잘못된 입력 값
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "c001", "Invalid input"),
    //이메일 중복
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U001", "Email already in use"),
    //닉네임 중복
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "U002", "Nickname alredy in use"),
    //리소스를 찾을 수 없음 -> 존재하지 않는 엔티티 접근
    NOT_FOUND(HttpStatus.NOT_FOUND, "C404", "Resource not found"),
    //서버 내부 오류
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
