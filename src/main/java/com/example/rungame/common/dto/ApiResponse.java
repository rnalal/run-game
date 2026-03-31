package com.example.rungame.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
* 공통 API 응답 DTO
*
* - 모든 REST API의 응답 포맷을 통일하기 위한 래퍼 객체
* - 성공/실패 여부, 실제 데이터, 메타 정보, 에러 메시지를 포함
*
* 프론트엔드에서 응답 파싱을 단순화하고
* API 일관성을 유지하기 위한 설계
* */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor  //Builder 사용 시 IDE 경고 방지 및 직렬화 대응
public class ApiResponse<T> {
    /*
    * 요청 성공 여부
    *
    * -true : 정상 처리
    * -false : 오류 발생
    * */
    private boolean success;
    /*
    * 실제 응답 데이터
    *
    * - 성공 시에만 사용
    * - 제네릭 타입으로 유연하게 확장
    * */
    private T data;
    /*
    * 부가 메타 정보
    * - 메시지, 페이징 정보 등 확장 가능
    * */
    private Meta meta;
    /*
    * 에러 메시지
    * - 실패 시에만 사용
    * */
    private String error;


    /*
    * 응답 메타 정보 내부 클래스
    * - 공통 메시지 관리용
    * */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Meta {
        //응답 메시지 ex)OK, ERROR
        private String message;
    }

    /*
    * 성공 응답 생성
    *
    * @param data : 응답 데이터
    * @return ApiResponse(success=true)
    * */
    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .meta(Meta.builder().message("OK").build())
                .build();
    }

    /*
    * 실패 응답 생성
    *
    * @param errorMessage : 에러 메시지
    * @return ApiResponse(success=false)
    * */
    public static ApiResponse<?> error(String errorMessage) {
        return ApiResponse.builder()
                .success(false)
                .error(errorMessage)
                .meta(Meta.builder().message("ERROR").build())
                .build();
    }
}