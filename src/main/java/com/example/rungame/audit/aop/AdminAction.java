package com.example.rungame.audit.aop;

import java.lang.annotation.*;

/*
* 관리자 행위 감사 어노테이션
*
* - 관리자가 수행항 주요 활동을 감사 로그로 기록하기 위한 메다데이터
* - AOP를 통해 메서드 실행 전/후를 가로채어 자동으로 기록
*
* 컨트롤러/서비스 로직과 감사 로직을 분리해서
* 비즈니스 코드의 가독성과 책임 분리를 유지
* */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AdminAction {

    //관리자 활동 이름
    //ex) USER_BAN_TOGLE, USER_ROLE_UPDATE
    String value();

    /*
    * 액션 대상 리소스 식별자
    *
    * - 어떤 리소스에 대해 수행된 액션인지 표현
    * ex) user, leaderboard
    * */
    String resource() default ""; // 리소스 식별 문자열 표현 템플릿
}
