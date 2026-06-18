package com.example.rungame.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

//CORS 설정
@Configuration
public class CorsConfig {

    //WebMvcConfigurer 기반 CORS 설정 Bean
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            //CORS 매핑 설정
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") //모든 API 경로에 대해 CORS 허용

                        //허용할 Origin
                        .allowedOrigins(
                                "http://localhost:8081",
                                "https://jaeyoung2.store",
                                "http://jaeyoung2.store"
                        )

                        //허용 HTTP 메서드
                        .allowedMethods("*")

                        //쿠키 포함 요청 허용
                        .allowCredentials(true)

                        //클라이언트에서 접근 가능한 응답 헤더
                        .exposedHeaders("Set-Cookie");
            }
        };
    }
}

