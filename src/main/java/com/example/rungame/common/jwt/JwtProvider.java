package com.example.rungame.common.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/*
 * JWT 생성 및 검증을 담당하는 Provider
 *
 * - Access / Refresh Token 생성
 * - 서명 및 만료 검증
 * - Claim 추출
 *
 * Controller / Filter / Service 계층에서
 * JWT 라이브러리에 직접 의존하지 않도록
 * JWT 관련 책임을 한 곳에 모은 클래스
 */
public class JwtProvider {

    //Claim Key 상수
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_VER  = "ver";  //tokenVersion
    public static final String CLAIM_TYP  = "typ";  //ACCESS/ REFRESH

    //Token Type
    public static final String TYPE_ACCESS  = "ACCESS";
    public static final String TYPE_REFRESH = "REFRESH";

    //JWT 설정 값
    private final SecretKey key;    //HMAC 서명 키
    private final String issuer;    //iss
    private final long accessTtlSeconds;    //Access Token TTL
    private final long refreshTtlSeconds;   //Refresh Token TTL

    /*
    * JwtProvider 생성자
    *
    * @param secret JWT 서명용 secret (환경 변수로 관리)
    * @param issuer JWT 발급자
    * @param accessTtlSeconds Access Token 유효 시간
    * @param refreshTtlSeconds Refresh Token 유효 시간
    * */
    public JwtProvider(String secret, String issuer, long accessTtlSeconds, long refreshTtlSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlSeconds = refreshTtlSeconds;
    }

    /*
    * Access Token 생성
    * - 사용자 식별 정보 포함
    * - API 인증에 사용
    * */
    public String createAccessToken(Long userId, String email, String nickname, String role, int tokenVersion) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(accessTtlSeconds);

        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(userId))    //sub = userId
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .id(UUID.randomUUID().toString())   //jti
                .claim(CLAIM_TYP, TYPE_ACCESS)      //토큰 타입
                .claim("email", email)
                .claim("nickname", nickname)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_VER, tokenVersion)     //즉시 무효화 전략
                .signWith(key)
                .compact();
    }

    /*
    * Refresh Token 생성
    * - 최소 Claim만 포함
    * - DB 저장 + Rotation 대상
    * */
    public String createRefreshToken(Long userId, String role, int tokenVersion) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(refreshTtlSeconds);

        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .id(UUID.randomUUID().toString()) //refresh도 jti 관리
                .claim(CLAIM_TYP, TYPE_REFRESH)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_VER, tokenVersion)
                .signWith(key)
                .compact();
    }

    /*
    * JWT 서명/ 만료 검증
    *
    * @param token JWT 무자열
    * @return 유효 여부
    * */
    public boolean validate(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            //만료, 서명 오류, 포맷 오류...
            System.out.println("JWT 유효성 실패: " + e.getClass().getSimpleName() + " / " + e.getMessage());
            return false;
        }
    }

    //사용자 id
    public Long getUserId(String token) {
        var claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
        return Long.parseLong(claims.getSubject());
    }

    //tokenVersion
    public int getVersion(String token) {
        var claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
        Integer v = claims.get(CLAIM_VER, Integer.class);
        return v == null ? 0 : v;
    }

    //토큰 타입
    public String getType(String token) {
        var claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
        return claims.get(CLAIM_TYP, String.class);
    }

    //사용자 역할
    public String getRole(String token) {
        var claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
        return claims.get(CLAIM_ROLE, String.class);
    }

    //JWT id (jti)
    public String getJti(String token) {
        var claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
        return claims.getId();
    }

    public SecretKey getKey() {
        return key;
    }

    //Authorization 헤더에서 Bearer 토큰 추출
    public String resolveBearer(String authHeader) {
        if (authHeader == null) return null;
        if (authHeader.startsWith("Bearer ")) return authHeader.substring(7);
        return null;
    }

    public long getAccessTtlSeconds() {
        return accessTtlSeconds;
    }
    public long getRefreshTtlSeconds() {
        return refreshTtlSeconds;
    }
}
