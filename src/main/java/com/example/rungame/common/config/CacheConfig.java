package com.example.rungame.common.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
* 캐시 설정 클래스
*
* - Spring Cache 추상화 활성화
* - 리더보드 조회 성능 향상을 위한 메모리 캐시 구성
*
* 현재는 ConcurrentMap 기반의 로컬 메모리 캐시를 사용,
* 추후 Redis 같은 외부 캐시로 쉽게 전환할 수 있도록 설계함
* */
@Configuration
@EnableCaching  //Spring Cache 기능 활성화
public class CacheConfig {

    /*
    * CacheManager Bean 등록
    * - 캐시 이름 단위로 분리 관리
    * - 리더보드 기간별 캐시를 명확히 분리
    *
    * @return CacheManager 구현체
    * */
    @Bean
    public CacheManager cacheManager() {
        /*
        * ConcurrentMapCacheManager
        * - 간단한 인메모리 캐시
        * - 단일 서버 환경에서 적합
        * */
        return new ConcurrentMapCacheManager(
                "leaderboard_all", "leaderboard_weekly", "leaderboard_monthly"
        );
    }
}
