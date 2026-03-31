package com.example.rungame.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/*
* CORS 설정
* - 프론트엔드와 백엔드 간 도메인이 다른 환경에서의 요청 허용
* - 쿠키 기반 인증(JWT Cookie)을 사용하기 위한 필수 설정
* */
@Configuration
public class CorsConfig {

    /*
    * WebMvcConfigurer 기반 CORS 설정 Bean
    *
    * Spring Security 이전 단계에서 적용,
    * 컨트롤러 전반에 공통으로 CORS 정책을 적용함
    * */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            //CORS 매핑 설정
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") //모든 API 경로에 대해 CORS 허용
                        /*
                        * 허용할 Origin
                        * - 개발 환경에서 프론트엔드 주소
                        * - 운영 시에는 실제 도메인으로 제한 필요
                        * */
                        .allowedOrigins("http://localhost:8081")
                        /*
                        * 허용 HTTP 메서드
                        * -GET, POST, PUT, PATCH, DELETE 등 전체 허용
                        * */
                        .allowedMethods("*")
                        /*
                        * 쿠키 포함 요청 허용
                        * - JWT를 HttpOnly Cookie로 사용하므로 필수
                        * - allowCredentials(true) 사용 시
                        *   allowedOrigins에 * 사용 불가
                        * */
                        .allowCredentials(true)
                        /*
                        * 클라이언트에서 접근 가능한 응답 헤더
                        * - Set-Cookie 헤더를 노출시켜
                        *   브라우저가 인증 쿠키를 정상 처리하도록 함
                        * */
                        .exposedHeaders("Set-Cookie");
            }
        };
    }
}

