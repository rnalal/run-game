package com.example.rungame.audit.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/*
 * AOP 설정 클래스
 *
 * - Spring AOP 기능 활성화
 * - @Aspect 기반 어드바이스가
 *   정상적으로 동작하도록 프록시 생성 허용
 *
 * 관리자 행위 감사를
 * 비즈니스 로직과 분리하기 위한 기반 설정
 */
@Configuration
@EnableAspectJAutoProxy
public class AopConfig {
}
