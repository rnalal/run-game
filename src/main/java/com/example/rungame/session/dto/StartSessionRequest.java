package com.example.rungame.session.dto;

import lombok.Data;

//세션 시작 요청 DTO
@Data
public class StartSessionRequest {

    //디바이스,환경 정보(JSON 문자열)
    private String deviceInfoJson;
}
