package com.example.rungame.leaderboard.service;

import com.example.rungame.leaderboard.dto.LeaderboardEntryDTO;
import com.example.rungame.leaderboard.repository.LeaderboardRepository;
import com.example.rungame.user.domain.User;
import com.example.rungame.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final LeaderboardRepository leaderboardRepository;
    private final RedisLeaderboardService redisLeaderboardService;

    //전체,7일,30일 리더보드 조회
    @Cacheable(
            value = "leaderboard_all",
            key = "#type + '-' + #range + '-' + #page + '-' + #size"
    )
    public Page<LeaderboardEntryDTO> getLeaderboard(String type, String range, int page, int size) {

        System.out.println("🔥 [CACHE MISS] Leaderboard 조회 실행 type=" + type + ", range=" + range);

        if("score".equals(type) && List.of("all", "daily", "weekly", "monthly").contains(range)) {
            try{
                Page<LeaderboardEntryDTO> redisResult =
                        redisLeaderboardService.getLeaderboard(range, page, size);

                if (!redisResult.isEmpty()) {
                    System.out.println("★★★★ REDIS LEADERBOARD HIT ★★★★");
                    return redisResult;
                }
                System.out.println("★★★★ REDIS EMPTY -> DB FALLBACK ★★★★");
            } catch (Exception e) {
                System.out.println("[LEADERBOARD] Redis 조회 실패 -> DB fallback: " + e.getMessage());
            }
        }

        //허용되지 않은 type이면 기본값 score로 보정
        if (!List.of("score", "distance", "coins").contains(type)) {
            type = "score";
        }

        PageRequest pageable = PageRequest.of(page, size);

        //최근 7일 리더보드
        if (range.equals("7d")) {
            return applyRanking(
                    leaderboardRepository.findLeaderboardInRange(type, LocalDateTime.now().minusDays(7), pageable),
                    page, size
            );
        }

        //최근 30일 리더보드
        if (range.equals("30d")) {
            return applyRanking(
                    leaderboardRepository.findLeaderboardInRange(type, LocalDateTime.now().minusDays(30), pageable),
                    page, size
            );
        }

        //전체 기간 리더보드
        return applyRanking(
                leaderboardRepository.findLeaderboard(type, pageable),
                page, size
        );
    }

    //조회된 페이지에 순위를 채워 넣는 헬퍼 메서드
    private Page<LeaderboardEntryDTO> applyRanking(Page<LeaderboardEntryDTO> result, int page, int size) {
        long startRank = (long) page * size + 1;

        for (int i = 0; i < result.getContent().size(); i++) {
            result.getContent().get(i).setRank(startRank + i);
        }
        return result;
    }

    //특정 유저의 전체 기간 기준 랭킹 조회
    public Map<String, Object> getUserRank(Long userId) {

        //세션이 한 번도 없으면
        if (leaderboardRepository.countAllSessionsByUser(userId) == 0) {
            Map<String, Object> response = new HashMap<>();

            response.put("userId", userId);
            response.put("rank", null);
            response.put("bestScore", null);
            response.put("message", "게임 기록 없음");

            return response;
        }

        //세션은 있지만 종료된 세션이 없어 점수를 계산할 수 없는 경우
        Integer bestScore = leaderboardRepository.findUserBestScore(userId);
        if (bestScore == null) {
            Map<String, Object> response = new HashMap<>();

            response.put("userId", userId);
            response.put("rank", null);
            response.put("bestScore", null);
            response.put("message", "종료된 세션 없음");

            return response;
        }

        //정상적으로 최고 점수와 순위를 계산할 수 있는 경우
        Long rank = leaderboardRepository.findUserRank(userId);
        if (rank == null) rank = 1L;    //방어적 기본값

        return Map.of(
                "userId", userId,
                "rank", rank,
                "bestScore", bestScore,
                "message", "정상 조회"
        );
    }
}
