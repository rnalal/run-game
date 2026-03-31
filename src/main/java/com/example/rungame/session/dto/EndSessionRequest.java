package com.example.rungame.session.dto;

import lombok.Data;

/*
* 세션 종료 요청 DTO
* - 클라이언트가 게임 끝났어요라고 서버에 알릴 때
*   종료 이유와 클라이언트 측 체크섬 정보를 함께 넘겨주는 요청 바디
*
* */
@Data
public class EndSessionRequest {

    /*
    * 세션 종료 사유
    * - hit, quit, timeout 등 클라이언트가 정의한 문자열 코드
    * */
    private String reason;

    /*
    * 클라이언트가 계산한 세션 이벤트 체크섬
    * - 서버 계산 값과 비교해서 무결성 검사에 사용
    * */
    private String clientChecksum;
}
