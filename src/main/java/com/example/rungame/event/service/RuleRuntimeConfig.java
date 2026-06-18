package com.example.rungame.event.service;

import lombok.Data;
import org.springframework.stereotype.Component;

//이벤트 룰 동작에 영향을 주는 런타임 설정 모음
@Component
@Data
public class RuleRuntimeConfig {
    //점프 최소 간격
    private int jumpMinIntervalMs = 250;
    //스프린트 최소 지속 시간
    private int sprintMinDurationMs = 500;
    //리버스 최대 지속 시간
    private int reverseMaxDurationMs = 5000;
    //체크포인트 점수 활성 여부
    private boolean enableCheckpointScore = true;
}
