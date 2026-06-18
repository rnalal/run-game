package com.example.rungame.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

//정적 리소스 전용 Security 설정
@Configuration
public class StaticResourceSecurityConfig {

    //정적 리소스 전용 FilterChain
    @Bean
    @Order(0)
    public SecurityFilterChain staticResources(HttpSecurity http) throws Exception {
        http
                //이 체인이 적용될 요청 경로
                .securityMatcher("/js/**", "/css/**", "/images/**", "/favicon.ico")

                //모든 요청 허용
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())

                //요청 캐시 비활성화
                .requestCache(rc -> rc.disable())

                //SecurityContext 비활성화
                .securityContext(sc -> sc.disable())

                //세션 관리 비활성화
                .sessionManagement(sm -> sm.disable())

                //CSRF 비활성화
                .csrf(csrf -> csrf.disable());

        return http.build();
    }
}

