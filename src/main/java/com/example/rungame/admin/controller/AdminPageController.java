package com.example.rungame.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/*
* 관리자 공통 페이지(view) 컨트롤러
*
* - 관리자 로그인 페이지
* - 관리자 메인(대시보드 진입) 페이지
*
* 화면 렌더링만 담당하며, 비즈니스 로직은 포함하지 않음
* */
@Controller
@RequestMapping("/rg-admin")
public class AdminPageController {

    /*
    * 관리자 로그인 페이지
    *
    * - 인증되지 않은 관리자가 접근하는 최초 화면
    * - 로그인 폼만 제공하는 view
    *
    * @return : 관리자 로그인 페이지 템플릿
    * */
    @GetMapping("/login")
    public String loginPage() {
        //resources/templates/admin/admin-login.html
        return "admin/admin-login";
    }

    /*
    * 관리자 메인 페이지
    *
    * - 로그인 성공 후 이동하는 관리자 홈 화면
    * - 각 관리 기능(대시보드, 이벤트, 리더보드 등)으로 진입하는 시작점
    *
    * @return : 관리자 메인 페이지 템플릿
    * */
    @GetMapping("/main")
    public String mainPage() {
        //resources/templates/admin/admin-main.html
        return "admin/admin-main";
    }
}
