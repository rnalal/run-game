package com.example.rungame.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/*
* 정적 리소스 전용 Security 설정
* - JS/ CSS/ 이미지/ favicon 등은
*   인증,인가,세션,보안 컨텍스트가 전혀 필요 없음
* - Spring Security 필터 체인을 가장 앞단에서 종료시켜
*   불필요한 보안 처리 비용을 제거
* */
@Configuration
public class StaticResourceSecurityConfig {

    /*
    * 정적 리소스 전용 FilterChain
    *
    * @Order(0)
    * - 가장 먼저 적용되는 체인
    * - 다른 SecurityFilterChain보다 우선 처리
    * */
    @Bean
    @Order(0)
    public SecurityFilterChain staticResources(HttpSecurity http) throws Exception {
        http
                /*
                * 이 체인이 적용될 요청 경로
                * - 정적 리소스만 대상
                * - 여기에 매칭되면 다른 Security 설정은 아예 타지 않음
                * */
                .securityMatcher("/js/**", "/css/**", "/images/**", "/favicon.ico")
                /*
                * 모든 요청 허용
                * - 정적 리소스는 인증/인가 개념 자체가 없음
                * */
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                /*
                * 요청 캐시 비활성화
                * - 인증 후 원래 요청으로 돌아가는 기능
                * - 정적 리소스에는 불필요
                * */
                .requestCache(rc -> rc.disable())
                /*
                * SecurityContext 비활성화
                * - Authentication 저장/조회 안 함
                * */
                .securityContext(sc -> sc.disable())
                /*
                * 세션 관리 비활성화
                * - 정적 리소스 요청에 세션 생성 방지
                * */
                .sessionManagement(sm -> sm.disable())
                /*
                * CSRF 비활성화
                * - 정적 리소스는 상태 변경 요청이 아님
                * */
                .csrf(csrf -> csrf.disable());

        return http.build();
    }
}

