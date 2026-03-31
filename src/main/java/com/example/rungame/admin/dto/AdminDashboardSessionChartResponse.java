package com.example.rungame.admin.dto;

import java.util.List;

/*
* 관리자 대시보드 세션 차트 응답 DTO
*
* - 기간별 세션 수 통계
* - 관리자 대시보드 차트 렌더링용 데이터 구조
* */
public record AdminDashboardSessionChartResponse(
    /*
    * 차트 X축 라벨
    *
    * - 날짜 또는 시간 단위 문자열
    *  (예: 2026-01-01, 10:00 등)
    * */
    List<String> labels,
    /*
    * 차트 데이터 값
    *
    * - 각 라벨에 해당하는 세션 수
    * - labels 순서와 1:1로 매칭됨
    * */
    List<Long> data
) { }
