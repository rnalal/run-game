package com.example.rungame.session.dto;

import lombok.Data;

//세션 종료 요청 DTO
@Data
public class EndSessionRequest {

    //세션 종료 사유
    private String reason;

    //클라이언트가 계산한 세션 이벤트 체크섬
    private String clientChecksum;
}
