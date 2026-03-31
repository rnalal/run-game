package com.example.rungame.admin.service;

import com.example.rungame.leaderboard.dto.LeaderboardEntryDTO;
import com.example.rungame.leaderboard.dto.LeaderboardSummaryResponse;
import com.example.rungame.leaderboard.repository.LeaderboardRepository;
import com.example.rungame.session.domain.Session;
import com.example.rungame.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/*
* 관리자 리더보드 관리 서비스
*
* - 리더보드 캐시 관리
* - 랭킹 기간 해석 (전체 / 7일 / 30일)
* - 리더보드 통계 요약
* - 관리자에 의한 사용자 점수 강제 수정
*
* 운영/관리 목적의 기능만을 담당하며,
* 일반 사용자 리더보드 로직과 분리되어 있음
* */
@Service
@RequiredArgsConstructor
public class AdminLeaderboardService {

    /*
    * Spring Cache 관리자
    *
    * - 리더보드 관련 캐시 제어에 사용
    * */
    private final CacheManager cacheManager;
    //리더보드 조회 전용 Repository
    private final LeaderboardRepository leaderboardRepository;
    /*
    * 세션 저장용 Repository
    *
    * - 관리자 점수 오버라이드 시 가상 세션 생성에 사용
    * */
    private final SessionRepository sessionRepository;

    //==================리더보드 캐시 갱신=====================
    /*
    * 리더보드 관련 캐시 전체 삭제
    *
    * - leaderboard로 시작하는 캐시만 선택적으로 제거
    * - 운영 중 점수 반영 지연 문제 대응
    * */
    public Map<String, Object> refreshCache() {
        List<String> cleared = new ArrayList<>();
        cacheManager.getCacheNames().forEach(name -> {
            if (name.startsWith("leaderboard")) {
                Objects.requireNonNull(cacheManager.getCache(name)).clear();
                cleared.add(name);
            }
        });
        return Map.of("cleared", cleared);
    }

    //=================랭킹 기간 범위 해석======================
    /*
    * 랭킹 기간 문자열을 날짜 범위로 변환
    *
    * @param range : "7d" | "30d" | "all"
    * */
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

    //=================리더보드 통계 요약========================
    /*
    * 리더보드 통계 요약 조회
    *
    * - 평균 점수
    * - 최고 점수
    * - 상위 1% 컷오프
    * - 점수 분포(히스토그램)
    * */
    public LeaderboardSummaryResponse summary(String range) {

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

    //===================점부 분포(히스토그램) 생성==================
    /*
    * 점수 분포 히스토그램 생성
    *
    * - 0~999 : 100점 단위 버킷
    * - 1000점 이상 : 하나의 버킷으로 처리
    * */
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

    //==================관리자 점수 강제 수정=========================
    /*
    * 사용자 최고 점수 관리자 강제 수정
    *
    * - 실제 플레이 없이 관리자용 가상 세션 생성
    * - 점수 조작/복구/운영 보정 용도
    * - 캐시 즉시 갱신
    * */
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
