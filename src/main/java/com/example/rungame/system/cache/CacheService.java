package com.example.rungame.system.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Objects;

//캐시 관리 서비스
@Service
@RequiredArgsConstructor
public class CacheService {

    //스프링 캐시 추상화를 통해 주입되는 캐시 매니저
    private final CacheManager cacheManager;

    //지정한 이름의 캐시를 비우는 메서드
    public boolean evictCache(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if(cache == null) return false;
        cache.clear();
        return true;
    }

    //사용 중인 모든 캐시를 한 번에 초기화하는 메서드
    public void evictAll() {
        if(cacheManager == null) return;
        for (String name : Objects.requireNonNull(cacheManager.getCacheNames())) {
            var cache = cacheManager.getCache(name);
            if (cache != null) cache.clear();
        }
    }
}
