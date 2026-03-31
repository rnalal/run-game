package com.example.rungame.common.config;

import com.example.rungame.common.jwt.JwtProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
* JWT 관련 설정 클래스
* - application.properties에 정의된 JWT 설정 값을 주입
* - JwtProvider를 Bean 으로 등록하여 전역에서 사용
*
* 인증/인가 로직과 설정을 분리하기 위한 구성 클래스
* */
@Configuration
public class JwtConfig {

    /*
    * JWTProvider Bean 등록
    *
    * @param secret : JWT 서명에 사용할 비밀 키
    * @param issuer : JWT 발급자(iss)
    * @param accessValidSeconds : Access Token 유효 시간(초)
    * @param refreshValidSeconds : Refresh Token 유효 시간(초)
    * @return JwtProvider 인스턴스
    * */
    @Bean
    public JwtProvider jwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.access-valid-seconds}") long accessValidSeconds,
            @Value("${jwt.refresh-valid-seconds}") long refreshValidSeconds
    ) {
        /*
        * JWTProvider는
        * - 토큰 생성
        * - 토큰 검증
        * - Claim 추출
        * 을 전담하는 컴포넌트
        *
        * 설정 값을 외부로 분리함으로써
        * - 환경별(secret/TTL) 설정 변경 용이
        * - 보안 정보 코드 분리
        * */
        return new JwtProvider(secret, issuer, accessValidSeconds, refreshValidSeconds);
    }
}
