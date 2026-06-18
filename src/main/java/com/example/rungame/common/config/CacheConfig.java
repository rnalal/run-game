package com.example.rungame.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

//캐시 설정 클래스
@Configuration
@EnableCaching  //Spring Cache 기능 활성화
public class CacheConfig {

    //CacheManager Bean 등록
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "leaderboard_all",
                "admin_leaderboard_summary",
                "admin_dashboard_summary",
                "admin_dashboard_session_chart",
                "admin_dashboard_event_chart"
        );

        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(500)
                        .expireAfterWrite(Duration.ofMinutes(10))
        );

        return cacheManager;
    }
}
