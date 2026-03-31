package com.example.rungame.admin.dto;

import java.time.LocalDateTime;

/*
* 관리자 사용자 활동 요약 응답 DTO
*
* - 특정 사용자의 활동 내역을 관리자 관점에서 요약
* - 사용자 행동 분석 및 운영 판단에 활용
* */
public record AdminUserActivityResponse(
    //사용자 ID
    Long userId,
    //마지막 로그인 시각
    LocalDateTime lastLoginAt,
    //조회 기간 내 세션 수
    long sessionCountInRange,
    //전체 누적 플레이 시간 (초 단위)
    long playSecondsTotal,
    //최근 N일 기준 누적 플레이 시간 (초 단위)
    long playSecondsInRange
) { }
