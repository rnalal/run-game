package com.example.rungame.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/rg-admin")
public class AdminPageController {

    //관리자 로그인 페이지
    @GetMapping("/login")
    public String loginPage() {
        return "admin/admin-login";
    }

    //관리자 메인 페이지
    @GetMapping("/main")
    public String mainPage() {
        return "admin/admin-main";
    }
}
