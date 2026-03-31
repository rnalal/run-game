package com.example.rungame.page;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/*
* 사용자 공용 페이지 라우팅 컨트롤러
* - 브라우저에서 들어오는 URL을 어떤 화면으로 보낼지 결정하는 페이지 진입 컨트롤러
* */
@Controller
public class PageController {

    //메인페이지
    @GetMapping("/")
    public String main() {
        return "users/game-main";
    }

    //로그인페이지
    @GetMapping("/login")
    public String loginPage() {
        return "users/login";
    }

    //회원가입페이지
    @GetMapping("/signup")
    public String signupPage() {
        return "users/signup";
    }

    /*
    * 마이페이지
    *
    * 접근 제어
    *   - @PreAuthorize("isAuthenticated()") : 로그인한 사용자만 접근 가능
    * */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/mypage")
    public String mypage() {
        return "users/mypage";
    }

    //게임페이지
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/game")
    public String game() {
        return "users/game";
    }
}
