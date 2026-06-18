package com.example.rungame.qna.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/rg-admin/qna")
public class QnaAdminPageController {

    // 관리자 QnA 목록 페이지
    @GetMapping
    public String adminQnaListPage() {
        return "admin/qna/admin-qna";
    }

    // 관리자 QnA 상세 + 답변 관리 페이지
    @GetMapping("/detail")
    public String adminQnaDetailPage() {
        return "admin/qna/admin-qnaDetail";
    }
}
