package com.example.rungame.admin.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/*
* 관리자 이벤트 관리 화면(view) 전용 컨트롤러
*
* - 이벤트 로그 관리 페이지(view) 렌더링 담당
* - REST API가 아닌 서버 사이드 view 반환용 컨트롤러
*
* ADMIN 또는 SUPER_ADMIN 권한을 가진 관리자만 접근 가능
* */
@Controller
@RequestMapping("/rg-admin/events")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminEventPageController {

    //관리자 이벤트 관리 페이지 진입
    //@return : 이벤트 관리 화면 템플릿 경로
    @GetMapping("/page")
    public String page() {
        return "admin/admin-events";
    }
}
