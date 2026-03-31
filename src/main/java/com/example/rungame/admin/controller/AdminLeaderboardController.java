package com.example.rungame.admin.controller;

import com.example.rungame.admin.service.AdminLeaderboardService;
import com.example.rungame.leaderboard.dto.LeaderboardSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/*
* 관리자 리더보드 관리 컨트롤러
*
* - 관리자 리더보드 페이지(view) 제공
* - 리더보드 캐시 관리
* - 랭킹 기간 범위 해석
* - 리더보드 통계 요약 조회
* - 사용자 점수 관리자 수동 수정(오버라이드)
*
* ADMIN 또는 SUPER_ADMIN 권한을 가진 관리자만 접근 가능
* */
@Controller
@RequestMapping("/rg-admin/leaderboard")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminLeaderboardController {

    //관리자 리더보드 관련 비즈니스 로직 담당 서비스
    private final AdminLeaderboardService adminLeaderboardService;

    //관리자 리더보드 화면 진입
    @GetMapping("/page")
    public String page(){
        return "admin/admin-leaderboard";
    }

    /*
    * 리더보드 캐시 수동 갱신
    *
    * - 관리자 요쳥 시 즉시 캐시 재계산
    * - 운영 중 랭킹 반영 지연 문제 대응
    * */
    @PostMapping("/cache/refresh")
    public ResponseEntity<Map<String, Object>> refreshCache() {
        return ResponseEntity.ok(adminLeaderboardService.refreshCache());
    }

    /*
    * 랭킹 기간 범위 조회
    *
    * - all / daily / weeily / monthly 등
    * - 관리자 필터 UI 구성 용도
    * */
    @GetMapping("/range")
    public ResponseEntity<Map<String, Object>> getRange(@RequestParam(defaultValue = "all") String range) {
        return ResponseEntity.ok(adminLeaderboardService.resolveRange(range));
    }

    /*
    * 리더보드 통계 요약 조회
    *
    * - 평균 점수
    * - 최고 점수
    * - 참여 유저 수 등
    * */
    @GetMapping("/stats/summary")
    public ResponseEntity<LeaderboardSummaryResponse> summary(
            @RequestParam(defaultValue = "all") String range
    ) {
        return ResponseEntity.ok(adminLeaderboardService.summary(range));
    }

    /*
    * 사용자 최고 점수 관리자 수동 수정 (오버라이드)
    *
    * - 부정 행위 대응
    * - 운영 이슈 보정 용도
    * */
    @PostMapping("/user/update-score")
    public ResponseEntity<Map<String, Object>> overrideUserScore(
            @RequestParam Long userId,
            @RequestParam int newBestScore
    ) {
        return ResponseEntity.ok(adminLeaderboardService.overrideUserBestScore(userId, newBestScore));
    }
}
