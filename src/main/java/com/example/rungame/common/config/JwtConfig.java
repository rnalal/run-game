package com.example.rungame.common.config;

import com.example.rungame.common.jwt.JwtProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//JWT 관련 설정 클래스
@Configuration
public class JwtConfig {

    //JWTProvider Bean 등록
    @Bean
    public JwtProvider jwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.access-valid-seconds}") long accessValidSeconds,
            @Value("${jwt.refresh-valid-seconds}") long refreshValidSeconds
    ) {

        return new JwtProvider(secret, issuer, accessValidSeconds, refreshValidSeconds);
    }
}
