package com.example.rungame.system.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Objects;

/*
* 캐시 관리 서비스
* - 스프링 캐시를 직접 건드리지 않고 이름으로 특정 캐시를 비우거나, 전체 캐시를
*   한 번에 초기화하는 관리용 서비스
*
* - 개별 캐시 비우기
*   -evictCache(cacheName)
*       - 주어진 캐시 이름에 해당하는 캐시 영역만 초기화함
*       - 존재하지 않는 캐시 이름이면 false를 반환해서 호출 측에서 분기 처리 가능하게 설계함
* - 전체 캐시 비우기
*   - evictAll()
*       - CacheManager가 알고 있는 모든 캐시 이름을 순회하면서 clear()
*       - 운영 도중 환경 설정 변경 후 캐시 싹 비우기 같은 버튼이나 관리자 API에서 호출하기 좋은 유틸 역할
*
* - 캐시 구현 기술에 직접 의존하지 않고 CacheManager를 감싸는 서비스 레이어를 두어서 운영 중에
*   개별,전체 캐시를 안전하게 초기화할 수 있게 함
*
* */
@Service
@RequiredArgsConstructor
public class CacheService {

    //스프링 캐시 추상화를 통해 주입되는 캐시 매니저
    private final CacheManager cacheManager;

    /*
    * 지정한 이름의 캐시를 비우는 메서드
    *
    * @param cacheName : 초기화할 캐시 이름
    * @return
    *   - true: 해당 이름의 캐시를 찾았고, clear()를 수행한 경우
    *   - false: 해당 이름의 캐시가 존재하지 않는 경우
    * */
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
