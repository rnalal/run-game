package com.example.rungame.admin.controller;

import com.example.rungame.admin.service.AdminLeaderboardService;
import com.example.rungame.leaderboard.dto.LeaderboardSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/rg-admin/leaderboard")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminLeaderboardController {

    private final AdminLeaderboardService adminLeaderboardService;

    @GetMapping("/page")
    public String page(){
        return "admin/admin-leaderboard";
    }

    //리더보드 캐시 수동 갱신
    @PostMapping("/cache/refresh")
    public ResponseEntity<Map<String, Object>> refreshCache() {
        return ResponseEntity.ok(adminLeaderboardService.refreshCache());
    }

    //랭킹 기간 범위 조회
    @GetMapping("/range")
    public ResponseEntity<Map<String, Object>> getRange(@RequestParam(defaultValue = "all") String range) {
        return ResponseEntity.ok(adminLeaderboardService.resolveRange(range));
    }

    //리더보드 통계 요약 조회
    @GetMapping("/stats/summary")
    public ResponseEntity<LeaderboardSummaryResponse> summary(
            @RequestParam(defaultValue = "all") String range
    ) {
        return ResponseEntity.ok(adminLeaderboardService.summary(range));
    }

    //사용자 최고 점수 관리자 수동 수정 (오버라이드)
    @PostMapping("/user/update-score")
    public ResponseEntity<Map<String, Object>> overrideUserScore(
            @RequestParam Long userId,
            @RequestParam int newBestScore
    ) {
        return ResponseEntity.ok(adminLeaderboardService.overrideUserBestScore(userId, newBestScore));
    }
}
