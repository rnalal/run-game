package com.example.rungame.event.domain;

/*
* 런게임 도메인의 모든 이벤트 타입을 정의하는 Enum
*
* - 게임 클라이언트에서 전송하는 event.type 값과 1:1로 매핑
* - 세션 이벤트 로그의 기준 값으로 사용
* - 통계, 대시보드, 리플레이, 디버깅 시 group by 축이 되는 핵심 도메인 상수
* */
public enum EventType {
    game_start,
    jump,
    coin_pick,
    hit_obstacle,
    game_over,
    sprint_start, sprint_end,
    slide_start, slide_end,
    reverse_start, reverse_end,
    pause, resume,
    powerup_pick,
    checkpoint
}
