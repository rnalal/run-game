package com.example.rungame.page;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

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

    //마이페이지
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
