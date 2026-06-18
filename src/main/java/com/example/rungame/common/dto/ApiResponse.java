package com.example.rungame.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

//공통 API 응답 DTO
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor  //Builder 사용 시 IDE 경고 방지 및 직렬화 대응
public class ApiResponse<T> {

    //요청 성공 여부
    private boolean success;

    //실제 응답 데이터
    private T data;

    //부가 메타 정보
    private Meta meta;

    //에러 메시지
    private String error;

    //응답 메타 정보 내부 클래스
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Meta {
        //응답 메시지
        private String message;
    }

    //성공 응답 생성
    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .meta(Meta.builder().message("OK").build())
                .build();
    }

    //실패 응답 생성
    public static ApiResponse<?> error(String errorMessage) {
        return ApiResponse.builder()
                .success(false)
                .error(errorMessage)
                .meta(Meta.builder().message("ERROR").build())
                .build();
    }
}