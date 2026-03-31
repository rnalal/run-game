package com.example.rungame.leaderboard.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/*
* 리더보드 관련 캐시를 주기적으로 비워주는 스케줄러
* - 리더보드 캐시가 너무 오래된 데이터를 들고 있지 않도록 1시간마다 캐시를
*   전체 초기화하는 작업을 자동으로 돌리는 컴포넌트
*
* - 리더보드는 읽기 빈도는 높고 쓰기는 이벤트 발생 시에만 생김
* - 조회 성능을 위해 캐시를 쓰되 완전히 오래된 정보가 계속 노출되지 않도록
*   일정 주기로 캐시를 강제로 비워주는 전략
*
* - @EnableScheduling으로 스케줄링 기능 활성화
* - @Scheduled(flixedRate = 3600000)으로 애플리케이션 구동 후 1시간마다
*   clearLeaderboardCache() 실행
* - 등록된 캐시 이름들을 돌면서 각 캐시를 clear()
* */
@Component
@EnableScheduling
@RequiredArgsConstructor
public class LeaderboardCacheScheduler {

    /*
    * 스프링 캐시 추상화를 사용하기 위한 CacheManager
    * - Caffeine, Redis, SimpleCache 등 어떤 캐시 구현체를 쓰더라도
    *   CacheManager만 주입받으면 공통 방식으로 캐시를 다룰 수 있음
    * */
    private final CacheManager cacheManager;

    /*
    * 리더보드 캐시 주기적 초기화 작업
    * - fixedRate = 3600000 ms -> 1시간마다 한 번씩 실행
    * - 1)현재 등록된 캐시 이름 목록을 가져온 뒤
    *   2)각 캐시가 존재하면 clear() 호출
    *   3)콘솔 로그로 시작/완료 출력
    *
    * 실제 운영 환경에서는 System.out 대신 log를 사용하는 것이 일반적이지만,
    * 여기서는 동작 확인용으로 간단히 출력하고 있음
    * */
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
