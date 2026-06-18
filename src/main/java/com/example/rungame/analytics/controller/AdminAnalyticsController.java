package com.example.rungame.analytics.controller;

import com.example.rungame.analytics.service.AnalyticsService;
import com.example.rungame.analytics.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/rg-admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;
    private final MonitoringService monitoringService;

    //사용자 성장 지표 조회
    @GetMapping("/user-growth")
    public Map<String, Object> userGrowth() {
        return analyticsService.userGrowthSummary();
    }

    //코인 사용량 분석
    @GetMapping("/coin-usage")
    public Map<String, Object> coinUsage() {
        return analyticsService.coinStats();
    }

    //이벤트 발생 빈도 통계
    @GetMapping("/event-frequency")
    public Map<String, Object> eventStats() {
        return Map.of("data", analyticsService.eventFrequency());
    }

    //서버 성능 지표 조회
    @GetMapping("/server-metrics")
    public Map<String, Object> metrics() {
        return monitoringService.serverMetrics();
    }
}
