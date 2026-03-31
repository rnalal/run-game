package com.example.rungame.admin.dto;

/*
* 관리자 대시보드 요약 정보 응답 DTP
*
* - 관리자 메인 대시보드 상단에 노출되는 핵심 지표 모음
* - 서비스 운영 상태를 한눈에 파악하기 위한 데이터 집합
* */
public record AdminDashboardSummaryResponse(
    //전체 사용자 수
    long userCount,
    //오늘 생성된 세션 수
    long todaySessionCount,
    //온르 사용된 코인 총량
    long todayCoinUsage,

    //일간 활성 사용자 수 (DAU)
    long dau,
    //주간 활성 사용자 수 (WAU)
    long wau,
    //월간 활성 사용자 수 (MAU)
    long mau,

    //오늘 신규 가입자 수
    long newUserToday,
    //최근 7일 신규 가입자 수
    long newUserWeek,

    //평균 플레이 시간 (텍스트 표현)
    String avgPlayTimeText,
    //평균 점수
    long avgScore,

    //크래시 발생 횟수
    long crashCount,
    //API 오류 발생 횟수
    long apiErrorCount,
    //현재 메모리 사용량(MB 단위)
    long memoryMb
) { }
