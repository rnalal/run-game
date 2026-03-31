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

/*
* 관리자 대시보드 전용 컨트롤러
*
* - 관리자 홈 화면에 필요한 요약 정보 제공
* - 세션 통계 차트 데이터 제공
* - 이벤트 통계 차트 데이터 제공
*
* ADMIN 또는 SUPER_ADMIN 권한을 가진 사용자만 접근 가능
* */
@RestController
@RequestMapping("/rg-admin/dashboard")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminDashboardController {

    //관리자 대시보드 관련 비즈니스 로직을 담당하는 서비스
    private final AdminDashboardService adminDashboardService;

    //생성자
    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    /*
    * 대시브도 상단 요약 정보 조회
    *
    * -전체 사용자 수
    * -전체 세션 수
    * -금일/최근 통계 등
    *
    * @return : 대시보드 요약 데이터
    * */
    @GetMapping("/summary")
    public AdminDashboardSummaryResponse summary() {
        //대시보드 요약 정보 조회를 서비스 계층에 위임
        return adminDashboardService.summary();
    }

    /*
    * 세션 통계 차트 데이터 조회
    *
    * - 일별/시간대별 세션 수 통계
    * - 대시보드 그래프 렌더링용 데이터
    *
    * @return : 세션 차트 데이터
    * */
    @GetMapping("/chart/sessions")
    public AdminDashboardSessionChartResponse sessionChart() {
        return adminDashboardService.getSessionChart();
    }

    /*
    * 이벤트 통계 차트 데이터 조회
    *
    * -jump, coin_pick, game_over 등 이벤트 발생 통계
    * -이벤트 분석 및 운영 모니터링 용도
    *
    * @return : 이벤트 차트 데이터
    * */
    @GetMapping("/chart/events")
    public AdminDashboardEventChartResponse eventChart() {
        return adminDashboardService.getEventChart();
    }
}
