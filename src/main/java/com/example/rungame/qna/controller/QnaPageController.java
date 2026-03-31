package com.example.rungame.qna.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/*
* QnA 사용자용 페이지 라우팅 컨트롤러
*
* - QnA 목록,상세,작성,수정,내 질문 보기 화면으로 URL을 깔끔하게 연결해 주는
*   페이지 전용 컨트롤러
* */
@Controller
@RequestMapping("/qna")
public class QnaPageController {

    // QnA 목록 페이지
    @GetMapping
    public String qnaListPage() {
        return "users/qna/qna";
    }

    // QnA 상세 페이지
    @GetMapping("/detail")
    public String qnaDetailPage() {
        return "users/qna/qna-detail";
    }

    // QnA 작성 페이지
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/write")
    public String qnaWritePage() {
        return "users/qna/qna-write";
    }

    // QnA 수정 페이지
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/edit")
    public String qnaEditPage() {
        return "users/qna/qna-edit";
    }

    // 내 질문 보기 페이지
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my")
    public String myQnaPage() {
        return "users/qna/qna-my";
    }
}
