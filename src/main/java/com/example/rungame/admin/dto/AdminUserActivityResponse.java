package com.example.rungame.admin.dto;

import java.time.LocalDateTime;

public record AdminUserActivityResponse(
    //사용자 ID
    Long userId,
    //마지막 로그인 시각
    LocalDateTime lastLoginAt,
    //조회 기간 내 세션 수
    long sessionCountInRange,
    //전체 누적 플레이 시간
    long playSecondsTotal,
    //최근 N일 기준 누적 플레이 시간
    long playSecondsInRange
) { }
