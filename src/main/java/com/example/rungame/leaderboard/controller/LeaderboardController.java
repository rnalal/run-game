package com.example.rungame.leaderboard.controller;

import com.example.rungame.leaderboard.dto.LeaderboardEntryDTO;
import com.example.rungame.leaderboard.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    //리더보드 조회
    @GetMapping
    public Page<LeaderboardEntryDTO> getLeaderboard(
            @RequestParam(defaultValue = "score") String type,
            @RequestParam(defaultValue = "all") String range,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return leaderboardService.getLeaderboard(type, range, page, size);
    }

    //특정 유저의 현재 랭킹 조회
    @GetMapping("/rank")
    public ResponseEntity<?> getUserRank(@RequestParam Long userId) {
        return ResponseEntity.ok(leaderboardService.getUserRank(userId));
    }

}
