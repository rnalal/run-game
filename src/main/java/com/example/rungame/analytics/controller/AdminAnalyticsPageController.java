package com.example.rungame.analytics.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/*
* 관리자 분석 페이지 컨트롤러
*
* - 관리자 분석 대시보드 화면 렌더링 담당
* - REST API와 분리된 view 전용 컨트롤러
*
* ADMIN 또는 SUPER_ADMIN 권한을 가진 관리자만 접근 가능
* */
@Controller
@RequestMapping("/rg-admin/analytics")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminAnalyticsPageController {

    /*
    * 관리자 분석 페이지 진입
    *
    * @return : 분석 대시보드 템플릿 경로
    *           (resources/templates/admin-analytics.html)
    * */
    @GetMapping("/page")
    public String page(){
        return "admin/admin-analytics";
    }
}
