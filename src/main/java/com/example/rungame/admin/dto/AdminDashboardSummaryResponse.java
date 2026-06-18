package com.example.rungame.admin.dto;


public record AdminDashboardSummaryResponse(
    //전체 사용자 수
    long userCount,
    //오늘 생성된 세션 수
    long todaySessionCount,
    //오늘 사용된 코인 총량
    long todayCoinUsage,

    //일간 활성 사용자 수
    long dau,
    //주간 활성 사용자 수
    long wau,
    //월간 활성 사용자 수
    long mau,

    //오늘 신규 가입자 수
    long newUserToday,
    //최근 7일 신규 가입자 수
    long newUserWeek,

    //평균 플레이 시간
    String avgPlayTimeText,
    //평균 점수
    long avgScore,

    //크래시 발생 횟수
    long crashCount,
    //API 오류 발생 횟수
    long apiErrorCount,
    //현재 메모리 사용량
    long memoryMb
) { }
