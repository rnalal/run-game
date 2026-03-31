package com.example.rungame.leaderboard.controller;

import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/*
* 리더보드 화면 진입용 컨트롤러
* - REST API가 아닌 템플릿을 반환하는 웹 컨트롤러
* - /leaderboard/page 로 들어온 요청을 리더보드 화면 뷰로 연결해 주는 역할
*
* REST 컨트롤러와의 차이
* - LeaderboardController: JSON 응답(API, 프론트 JS가 호출)
* - LeaderboardPageController: 뷰 이름 반환
* */
@Controller
@RequestMapping("/leaderboard")
@RequiredArgsConstructor
public class LeaderboardPageController {

    /*
    * 리더보드 페이지 진입
    *
    * - GET /leaderboard/page 요청 시
    *   users/leaderboard 템플릿을 반환
    *
    * @return : users/leaderboard
    * -> ViewResolver가 users/leaderboard.html 같은 실제 템플릿을 찾아 렌더링
    * */
    @GetMapping("/page")
    @PermitAll
    public String leaderboardPage() {
        return "users/leaderboard";
    }
}
