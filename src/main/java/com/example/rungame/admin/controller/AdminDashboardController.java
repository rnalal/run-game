package com.example.rungame.admin.controller;

import com.example.rungame.admin.dto.AdminDashboardEventChartResponse;
import com.example.rungame.admin.dto.AdminDashboardSessionChartResponse;
import com.example.rungame.admin.dto.AdminDashboardSummaryResponse;
import com.example.rungame.admin.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rg-admin/dashboard")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    //대시보드 상단 요약 정보 조회
    @GetMapping("/summary")
    public AdminDashboardSummaryResponse summary() {
        return adminDashboardService.summary();
    }

    //세션 통계 차트 데이터 조회
    @GetMapping("/chart/sessions")
    public AdminDashboardSessionChartResponse sessionChart() {
        return adminDashboardService.getSessionChart();
    }

    //이벤트 통계 차트 데이터 조회
    @GetMapping("/chart/events")
    public AdminDashboardEventChartResponse eventChart() {
        return adminDashboardService.getEventChart();
    }
}
