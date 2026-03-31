package com.example.rungame.session.dto;

import lombok.Data;

/*
* 세션 시작 요청 DTO
* - 클라이언트가 새 게임을 시작할게요.라고 요청할 때
*   난이도,클라이언트 버전, 디바이스 정보를 함께 넘겨주는 요청 바디
* - POST /api/sessions/start 요청의 body로 사용
*
* - 어떤 난이도로 시작했는지 기록하고
* - 어떤 버전의 클라이언트에서 들어온 요청인지 남기고
* - 디바이스 정보를 세션,로그에 함께 저장할 수 있음
* */
@Data
public class StartSessionRequest {

    //게임 난이도 또는 스테이지 정보(선택)
    private String level;

    //현재 플레이중인 클라이언트 정보(선택)
    private String clientVersion;

    //디바이스,환경 정보(JSON 문자열, 선택)
    private String deviceInfoJson;
}
