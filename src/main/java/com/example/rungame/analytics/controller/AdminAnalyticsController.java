package com.example.rungame.analytics.controller;

import com.example.rungame.analytics.service.ABTestService;
import com.example.rungame.analytics.service.AnalyticsService;
import com.example.rungame.analytics.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/*
* 관리자 분석 컨트롤러
*
* - 사용자 성장 지표 분석
* - 코인 사용량 분석
* - 이벤트 발생 빈도 통계
* - 서버 성능 및 오류 지표 모니터링
* - A/B 테스트 그룹 배정 및 통계 조회
*
* 서비스 운영 및 데이터 기반 의사결정을 지원하는
* 관리자 전용 분석 API
* */
@RestController
@RequestMapping("/rg-admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminAnalyticsController {

    //사용자/게임 플레이 데이터 분석 서비스
    private final AnalyticsService analyticsService;
    //서버 상태 및 성능 모니터링 서비스
    private final MonitoringService monitoringService;
    //A/B 테스트 관리 서비스
    private final ABTestService abTestService;

    //==================사용자 성장 분석===================
    /*
    * 사용자 성장 지표 조회
    *
    * - 일/주/월 기준 사용자 증가 추이
    * - 관리자 분석 대시보드 차트용
    * */
    @GetMapping("/user-growth")
    public Map<String, Object> userGrowth() {
        return analyticsService.userGrowthSummary();
    }

    //=================코인 사용량 분석=====================
    /*
    * 코인 사용량 분석
    *
    * - 일별 코인 소모량
    * - 주요 소비 패턴 분석
    * */
    @GetMapping("/coin-usage")
    public Map<String, Object> coinUsage() {
        return analyticsService.coinStats();
    }

    //===============이벤트 사용 통계=======================
    /*
    * 이벤트 발생 빈도 통계
    *
    * - 점프, 코인 획득, 충돌 등 이벤트별 발생 횟수
    * - 게임 밸런스 및 UX 분석
    * */
    @GetMapping("/event-frequency")
    public Map<String, Object> eventStats() {
        return Map.of("data", analyticsService.eventFrequency());
    }

    //================서버 모니터링===========================
    /*
    * 서버 성능 지표 조회
    *
    * - 응답 시간
    * - 오류율
    * - 시스템 상태 모니터링
    * */
    @GetMapping("/server-metrics")
    public Map<String, Object> metrics() {
        return monitoringService.serverMetrics();
    }

    //==================A/B 테스트============================
    /*
    * 사용자 A/B 테스트 그룹 배정
    *
    * - 사용자 ID 기준으로 테스스 그룹 할당
    * - 기능/밸런스 실험용
    * */
    @GetMapping("/ab/group")
    public Map<String, Object> assign(@RequestParam Long userId) {
        String group = abTestService.assignUser(userId);
        return Map.of("userId", userId, "group", group);
    }
    /*
    * A/B 테스트 그룹별 통계 조회
    *
    * - 각 그룹의 성과 비교
    * */
    @GetMapping("/ab/stats")
    public Map<String, Object> abStats() {
        return abTestService.getGroupStats();
    }
}
