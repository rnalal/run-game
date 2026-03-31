package com.example.rungame.admin.dto;

import java.util.List;

/*
* 관리자 대시보드 이벤트 차트 응답 DTO
*
* - 이벤트 타입별 발생 횟수 통계
* - 관리자 대시보드 차트 렌더링용 데이터 구조
* */
public record AdminDashboardEventChartResponse(
    /*
    * 차트 X축 라벨
    *
    * - 이벤트 타입명(예: JUMP, COIN_PICK, GAME_OVER 등)
    * */
    List<String> labels,
    /*
    * 차트 데이터 값
    *
    * - 각 이벤트 타입별 발생 횟수
    * - labels 순서와 매칭됨
    * */
    List<Long> data
) { }
