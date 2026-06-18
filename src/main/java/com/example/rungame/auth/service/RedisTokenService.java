package com.example.rungame.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RedisTokenService {

    private final StringRedisTemplate redisTemplate;

    private static final String TOKEN_VERSION_PREFIX = "auth:ver:";
    private static final String REFRESH_PREFIX = "auth:refresh:";
    private static final String USER_REFRESH_PREFIX = "auth:user-refresh:";
    private static final String ACCESS_BLACKLIST_PREFIX = "auth:blacklist:access:";

    //사용자 토큰 버전을 Redis에 저장
    public void saveTokenVersion(Long userId, int tokenVersion) {
        redisTemplate.opsForValue().set(
                TOKEN_VERSION_PREFIX + userId,
                String.valueOf(tokenVersion)
        );
    }

    //Redis에 저장된 사용자 토큰 버전 조회
    public Integer getTokenVersion(Long userId) {
        String value = redisTemplate.opsForValue().get(TOKEN_VERSION_PREFIX + userId);
        if (value == null) return null;
        return Integer.parseInt(value);
    }

    //Refresh Token 및 사용자별 토큰 목록 저장
    public void saveRefreshToken(Long userId, String refreshJti, int tokenVersion, long ttlSeconds) {
        String refreshKey = REFRESH_PREFIX + refreshJti;
        String userRefreshKey = USER_REFRESH_PREFIX + userId;

        redisTemplate.opsForValue().set(
                refreshKey,
                userId + ":" + tokenVersion,
                Duration.ofSeconds(ttlSeconds)
        );

        redisTemplate.opsForSet().add(userRefreshKey, refreshJti);
        redisTemplate.expire(userRefreshKey, Duration.ofSeconds(ttlSeconds));
    }

    //Refresh Token 존재 여부 확인
    public boolean existsRefreshToken(String refreshJti) {
        Boolean exists = redisTemplate.hasKey(REFRESH_PREFIX + refreshJti);
        return Boolean.TRUE.equals(exists);
    }

    //Refresh Token 저장 값 조회
    public String getRefreshTokenValue(String refreshJti) {
        return redisTemplate.opsForValue().get(REFRESH_PREFIX + refreshJti);
    }

    //특정 Refresh Token 삭제
    public void deleteRefreshToken(Long userId, String refreshJti) {
        redisTemplate.delete(REFRESH_PREFIX + refreshJti);
        redisTemplate.opsForSet().remove(USER_REFRESH_PREFIX + userId, refreshJti);
    }

    //사용자의 모든 Refresh Token 삭제(전체 로그아웃)
    public void deleteAllRefreshTokens(Long userId) {
        String userRefreshKey = USER_REFRESH_PREFIX + userId;
        Set<String> refreshJtis = redisTemplate.opsForSet().members(userRefreshKey);

        if (refreshJtis != null) {
            for (String jti : refreshJtis) {
                redisTemplate.delete(REFRESH_PREFIX + jti);
            }
        }

        redisTemplate.delete(userRefreshKey);
    }

    //Access Token을 블랙리스트에 등록
    public void addAccessTokenBlacklist(String accessJti, long ttlSeconds) {
        if (accessJti == null || accessJti.isBlank()) return;
        if (ttlSeconds <= 0) return;

        redisTemplate.opsForValue().set(
                ACCESS_BLACKLIST_PREFIX + accessJti,
                "logout",
                Duration.ofSeconds(ttlSeconds)
        );
    }

    //Access Token 블랙리스트 등록 여부 확인
    public boolean isAccessTokenBlacklisted(String accessJti) {
        Boolean exists = redisTemplate.hasKey(ACCESS_BLACKLIST_PREFIX + accessJti);
        return Boolean.TRUE.equals(exists);
    }


}
