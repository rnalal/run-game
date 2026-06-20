package com.example.rungame.admin.service;

import com.example.rungame.leaderboard.dto.LeaderboardEntryDTO;
import com.example.rungame.leaderboard.dto.LeaderboardSummaryResponse;
import com.example.rungame.leaderboard.repository.LeaderboardRepository;
import com.example.rungame.session.domain.Session;
import com.example.rungame.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminLeaderboardService {

    //spring Cache 관리자
    private final CacheManager cacheManager;
    private final LeaderboardRepository leaderboardRepository;
    private final SessionRepository sessionRepository;

    //리더보드 관련 캐시 전체 삭제
    public Map<String, Object> refreshCache() {
        List<String> cleared = new ArrayList<>();

        cacheManager.getCacheNames().forEach(name -> {
            if (
                    name.startsWith("admin_leaderboard")
                    || name.startsWith("admin_dashboard")
            ) {
                Objects.requireNonNull(cacheManager.getCache(name)).clear();
                cleared.add(name);
            }
        });
        return Map.of("cleared", cleared);
    }

    //랭킹 기간 문자열을 날짜 범위로 변환
    public Map<String, Object> resolveRange(String range) {
        LocalDateTime start = switch (range) {
            case "7d" -> LocalDateTime.now().minusDays(7);
            case "30d" -> LocalDateTime.now().minusDays(30);
            default -> null; // 전체 기간
        };

        Map<String, Object> result = new HashMap<>();
        result.put("range", range);
        result.put("startDate", start);

        return result;
    }

    //리더보드 통계 요약 조회
    @Cacheable(
            value = "admin_leaderboard_summary",
            key = "#range"
    )
    public LeaderboardSummaryResponse summary(String range) {

        System.out.println("🔥 [CACHE MISS] AdminLeaderboard summary DB 조회 실행 range=" + range);

        //날짜 범위 계산
        Map<String, Object> rangeInfo = resolveRange(range);
        LocalDateTime startDate = (LocalDateTime) rangeInfo.get("startDate");

        //최고 점수 기준 정렬된 랭킹 목록
        List<LeaderboardEntryDTO> bestList = leaderboardRepository.findAllBestScoresDesc(startDate);
        if (bestList == null) {
            bestList = List.of();
        }

        long totalUsers = bestList.size();

        //최고 점수
        Integer maxScore = leaderboardRepository.maxScoreInRange(startDate);
        if (maxScore == null) maxScore = 0;

        //평균 점수
        double avg = 0.0;
        if (totalUsers > 0) {
            long sum = bestList.stream()
                    .mapToInt(LeaderboardEntryDTO::getBestScore)
                    .sum();
            avg = sum / (double) totalUsers;
        }

        //상위 1% 점수 컷오프
        int p99 = 0;
        if (totalUsers > 0) {
            int idx = (int) Math.ceil(totalUsers * 0.01) - 1;
            idx = Math.max(0, Math.min(idx, bestList.size() - 1));
            p99 = bestList.get(idx).getBestScore();
        }

        //점수 분포 히스토그램
        Map<String, Long> hist = buildHistogram(bestList);

        return LeaderboardSummaryResponse.builder()
                .range(range)
                .totalUsers(totalUsers)
                .avgBestScore(avg)
                .maxScore(maxScore)
                .top1PercentScore(p99)
                .histogram(hist)
                .build();
    }

    //점수 분포 히스토그램 생성
    private Map<String, Long> buildHistogram(List<LeaderboardEntryDTO> list) {
        Map<String, Long> buckets = new LinkedHashMap<>();

        for (int start = 0; start <= 900; start += 100) {
            buckets.put(start + "-" + (start + 99), 0L);
        }
        buckets.put("1000+", 0L);

        for (LeaderboardEntryDTO e : list) {
            int s = e.getBestScore();
            if (s >= 1000) {
                buckets.put("1000+", buckets.get("1000+") + 1);
            } else {
                int base = (s / 100) * 100;
                String key = base + "-" + (base + 99);
                buckets.computeIfPresent(key, (k, v) -> v + 1);
            }
        }

        return buckets;
    }

    //사용자 최고 점수 관리자 강제 수정
    @Transactional
    public Map<String, Object> overrideUserBestScore(Long userId, int newBestScore) {

        //관리자 오버라이드용 가상 세션 생성
        Session s = Session.builder()
                .userId(userId)
                .status(Session.Status.ENDED)
                .startedAt(LocalDateTime.now().minusSeconds(3))
                .endedAt(LocalDateTime.now())
                .score(newBestScore)
                .distance(0)
                .coins(0)
                .maxSpeed(null)
                .flags("ADMIN_OVERRIDE")
                .deviceInfo("{}")
                .build();

        sessionRepository.saveAndFlush(s);

        //리더보드 캐시 갱신
        refreshCache();

        return Map.of(
                "userId", userId,
                "newBestScore", newBestScore,
                "sessionId", s.getId(),
                "note", "ADMIN_OVERRIDE session added"
        );
    }
}
