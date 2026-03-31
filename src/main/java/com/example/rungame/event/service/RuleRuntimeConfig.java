package com.example.rungame.event.service;

import lombok.Data;
import org.springframework.stereotype.Component;

/*
* 이벤트 룰 동작에 영향을 주는 런타임 설정 모음
* - 점프 최소 간격, 스프린트 최소 유지시간, 리버스 최대 유지시간, 체크포인트 점수 활성 여부처럼
*   자주 조정하고 싶은 값들을 한 곳에 모아둔 설정 객체
*
* 특징
* - @Component 로 등록되어 스프링 빈으로 사용
* - @Data 로 getter/setter가 자동 생성되어 향후 관리자 페이지나 설정 API에서 값을 바꿔 줄 수도 있음
* */
@Component
@Data
public class RuleRuntimeConfig {
    /*
    * 점프 최소 간격
    * - 점프 이벤트가 너무 짧은 간격으로 연속해서 들어오지 않도록 막는 기준값
    * */
    private int jumpMinIntervalMs = 250;
    /*
    * 스프린트 최소 지속 시간
    * - sprint_start ~ sprint_end 사이의 시간이
    *   이 값보다 짧으면 의미 없는 토글로 간주하고 막는 데 쓸 수 있는 기준
    * - Sprint 관련 룰에서 사용해 스프린트는 최소 이 정도는 유지돼야 한다는 정책을 표현
    * */
    private int sprintMinDurationMs = 500;
    /*
    * 리버스 최대 지속 시간
    * - reverse_start 이후 reverse 상태를 최대 몇 ms까지 허용할지 정하는 상한
    * - 너무 오래 역주행 상태를 유지하면서 점수를 버그성으로 쌓는 상황을 막는 용도
    * */
    private int reverseMaxDurationMs = 5000;
    /*
    * 체크포인트 점수 활성 여부
    * - true: checkpoint 이벤트에 점수를 부여하는 로직을 켬
    * - false: checkpoint는 로그/분석용으로만 사용하고 점수는 주지 않음
    * */
    private boolean enableCheckpointScore = true;
}
