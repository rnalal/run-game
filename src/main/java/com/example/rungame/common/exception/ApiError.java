package com.example.rungame.common.exception;

import lombok.Builder;
import lombok.Getter;
import org.springframework.validation.FieldError;

import java.time.Instant;
import java.util.List;

//공통 API 에러 응답 모델
@Getter
@Builder
public class ApiError {

    //성공 여부
    private final boolean success = false;
    //에러 코드
    private final String code;
    //사용자에게 전달할 에러 메시지
    private final String message;
    //HTTP 상태 코드
    private final int status;
    //요청 경로
    private final String path;
    //에러 발생 시각
    private final Instant timestamp;
    //필드 단위 검증 에러 목록
    private final List<FieldError> errors;

    //필터 검증 에러 정보
    @Getter @Builder
    public static class FieldError {
        //검증 실패한 필드명
        private final String field;
        private final String reason;
    }
}
