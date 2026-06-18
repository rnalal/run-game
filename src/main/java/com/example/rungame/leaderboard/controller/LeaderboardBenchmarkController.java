package com.example.rungame.leaderboard.controller;

import com.example.rungame.leaderboard.dto.LeaderboardEntryDTO;
import com.example.rungame.leaderboard.repository.LeaderboardRepository;
import com.example.rungame.leaderboard.service.LeaderboardService;
import com.example.rungame.leaderboard.service.RedisLeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/benchmark/leaderboard")
@RequiredArgsConstructor
public class LeaderboardBenchmarkController {

    private final LeaderboardRepository leaderboardRepository;
    private final RedisLeaderboardService redisLeaderboardService;
    private final LeaderboardService leaderboardService;

    @GetMapping("/db")
    public Page<LeaderboardEntryDTO> db(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        long start = System.currentTimeMillis();

        Page<LeaderboardEntryDTO> result =
                leaderboardRepository.findLeaderboard(
                        "score",
                        PageRequest.of(page, size)
                );

        long end = System.currentTimeMillis();
        System.out.println("⏱️ [DB 직접 조회] 실행시간 = " + (end - start) + " ms");

        return result;
    }

    @GetMapping("/redis")
    public Page<LeaderboardEntryDTO> redis(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        long start = System.currentTimeMillis();

        Page<LeaderboardEntryDTO> result =
                redisLeaderboardService.getLeaderboard("all", page, size);

        long end = System.currentTimeMillis();
        System.out.println("⏱️ [Redis Sorted Set 조회] 실행시간 = " + (end - start) + " ms");

        return result;
    }

    @GetMapping("/cache")
    public Page<LeaderboardEntryDTO> cache(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        long start = System.currentTimeMillis();

        Page<LeaderboardEntryDTO> result =
                leaderboardService.getLeaderboard("score", "all", page, size);

        long end = System.currentTimeMillis();
        System.out.println("⏱️ [Spring Cache 조회] 실행시간 = " + (end - start) + " ms");

        return result;
    }
}