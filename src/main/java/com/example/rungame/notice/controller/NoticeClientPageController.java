package com.example.rungame.notice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class NoticeClientPageController {

    //공지 목록 페이지
    @GetMapping("/notices")
    public String noticeListPage() {
        return "users/notice-list";
    }

    //공지 상세 페이지 진입
    @GetMapping("/notices/{id}")
    public String noticeDetailPage(@PathVariable Long id) {
        return "users/notice-detail";
    }
}
