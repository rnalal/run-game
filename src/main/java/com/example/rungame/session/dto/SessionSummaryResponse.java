package com.example.rungame.session.dto;

import lombok.Builder;
import lombok.Data;

//세션 종료 후 클라이언트에 내려주는 요약 응답 DTO
@Data
@Builder
public class SessionSummaryResponse {
    private Long sessionId;
    private int score;
    private int coins;
    private int distance;
    private Double maxSpeed;
    //실제 플레이 시간
    private long activeDurationMs;
    //체크섬 검증 결과
    private boolean checksumValid;
    //세션 중 감지된 이상 플래그 목록
    private String flags;
}
