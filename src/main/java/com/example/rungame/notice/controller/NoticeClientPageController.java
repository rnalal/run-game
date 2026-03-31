package com.example.rungame.notice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/*
* 공지사항 화면 전용 페이지 컨트롤러
* - REST API가 아니라 공지 리스트/상세를 보여주는 HTML 페이지로 라우탕하는 역할
*
* - /notices -> 공지 목록 화면
* - /notices/{id} -> 공지 상세 화면
*
* 실제 데이터는 별도의 Notice API 혹은 서버사이드 템플릿에서 불러와 그려주는 구조로 확장 가능
* - URL 구조를 먼저 깔끔하게 잡아두고 뷰 이름을 명시해서 클라이언트가 어떤 화면으로 가야 하는지 분리해 둔 컨트롤러
* */
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
