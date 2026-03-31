package com.example.rungame.qna.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/*
* 관리자 QnA 관리 화면 라우팅 컨트롤러
* - 관리자가 브라우저로 접속했을 때 어떤 QnA 관리 화면을 보여줄지 연결해 주는 페이지 전용 컨트롤러
* */
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
