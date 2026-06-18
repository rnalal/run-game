package com.example.rungame.leaderboard.controller;

import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/leaderboard")
@RequiredArgsConstructor
public class LeaderboardPageController {

    @GetMapping("/page")
    @PermitAll
    public String leaderboardPage() {
        return "users/leaderboard";
    }
}
