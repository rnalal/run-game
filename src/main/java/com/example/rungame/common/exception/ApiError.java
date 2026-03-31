package com.example.rungame.common.exception;

import lombok.Builder;
import lombok.Getter;
import org.springframework.validation.FieldError;

import java.time.Instant;
import java.util.List;

/*
* 공통 API 에러 응답 모델
*
* - 예외 발생 시 클라이언트로 내려주는 표준 에러 포맷
* - HTTP 상태 코드 + 에러 코드 + 메시지 + 요청 경로 + 시간 정보 포함
*
* ApiResponse와 역할을 분리하여
* 정상 응답과 에러 응답을 명확히 구분함
* */
@Getter
@Builder
public class ApiError {
    /*
    * 성공 여부
    * - 에러 응답이므로 항상 false
    * - 프론트엔드에서 ApiResponse와 동일한 방식으로 처리 가능
    * */
    private final boolean success = false;
    /*
    * 에러 코드
    * - 비즈니스/도메인 단위의 에러 식별자
    * ex) AUTH_001, USER_404
    * */
    private final String code;
    //사용자에게 전달할 에러 메시지
    private final String message;
    //HTTP 상태 코드
    private final int status;
    /*
    * 요청 경로
    * - 어떤 API에서 오류가 발생했는지 추적용
    * */
    private final String path;
    //에러 발생 시각
    private final Instant timestamp;
    /*
    * 필드 단위 검증 에러 목록
    * - @Vaild 실패 시 사용
    * */
    private final List<FieldError> errors;

    /*
    * 필터 검증 에러 정보
    * - 어떤 필드가 왜 실패했는지를 멷확히 전달하기 위한 구조
    * */
    @Getter @Builder
    public static class FieldError {
        //검증 실패한 필드명
        private final String field;
        private final String reason;
    }
}
