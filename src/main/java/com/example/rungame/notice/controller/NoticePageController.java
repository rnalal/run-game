package com.example.rungame.notice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/*
* 관리자 공지사항 관리 화면 진입 컨트롤러
* - /admin/notices/page 요청을 받아 관리자용 공지 관리 화면 뷰로 보내주는 페이지 전용 컨트롤러
*
* - REST API가 아닌 서버사이드 템플릿을 반환하는 컨트롤러
* - 실제 공지 데이터 CRUD는 NoticeController에서 처리하고
*   이 컨트롤러는 관리자 페이지로 들어가는 입구만 담당
* */
@Controller
@RequestMapping("/rg-admin/notices")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class NoticePageController {

    @GetMapping("/page")
    public String page() {
        return "admin/admin-notices";
    }
}
