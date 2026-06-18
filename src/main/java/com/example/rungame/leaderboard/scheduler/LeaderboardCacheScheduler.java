package com.example.rungame.leaderboard.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

//리더보드 관련 캐시를 주기적으로 비워주는 스케줄러
@Component
@EnableScheduling
@RequiredArgsConstructor
public class LeaderboardCacheScheduler {

    //스프링 캐시 추상화를 사용하기 위한 CacheManager
    private final CacheManager cacheManager;

    //리더보드 캐시 주기적 초기화 작업
    @Scheduled(fixedRate = 3600000)
    public void clearLeaderboardCache() {
        System.out.println("[CACHE] 리더보드 캐시 초기화 시작");

        cacheManager.getCacheNames().forEach(name -> {
            if(cacheManager.getCache(name) != null) {
                cacheManager.getCache(name).clear();
                System.out.println("[CACHE] Cleared: " + name);
            }
        });

        System.out.println("[CACHE] 리더보드 캐시 초기화 완료");
    }
}
