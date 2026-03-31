package com.example.rungame.admin.service;

import com.example.rungame.event.service.RuleRuntimeConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/*
* 관리자 이벤트 룰 관리 서비스
*
* - 게임 이벤트 검증 및 점수 계산에 사용되는
*   런타임 룰 조회 및 생성
*
* 서버 재시작 없이 운영 중 룰을 조정할 수 있도록 설계
* */
@Service
@RequiredArgsConstructor
public class AdminEventRuleService {

    /*
    * 이벤트 룰 런타임 설정 객체
    *
    * - 애플리케이션 실행 중 동적으로 변경 가능
    * */
    private final RuleRuntimeConfig cfg;

    /*
    * 현재 이벤트 룰 조회
    *
    * @return : 이벤트 룰 설정 값 Map
    * */
    public Map<String, Object> current() {
        return Map.of(
                "jumpMinIntervalMs", cfg.getJumpMinIntervalMs(),
                "sprintMinDurationMs", cfg.getSprintMinDurationMs(),
                "reverseMaxDurationMs", cfg.getReverseMaxDurationMs(),
                "enableCheckpointScore", cfg.isEnableCheckpointScore()
        );
    }

    /*
    * 이벤트 룰 업데이트
    *
    * - 전달된 값만 부분적으로 반영
    * - null 값은 기존 설정 유지
    *
    * @return : 업데이트 이후 이벤트 룰 설정
    * */
    public Map<String, Object> update(
            Integer jumpMinIntervalMs,
            Integer sprintMinDurationMs,
            Integer reverseMaxDurationMs,
            Boolean enableCheckpointScore
    ) {
        //점프 최소 간격 업데이트
        if (jumpMinIntervalMs != null) cfg.setJumpMinIntervalMs(jumpMinIntervalMs);
        //스프린트 최소 지속 시간 업데이트
        if (sprintMinDurationMs != null) cfg.setSprintMinDurationMs(sprintMinDurationMs);
        //리버스 최대 지속 시간 업데이트
        if (reverseMaxDurationMs != null) cfg.setReverseMaxDurationMs(reverseMaxDurationMs);
        //체크포인트 점수 활성화 여부 업데이트
        if (enableCheckpointScore != null) cfg.setEnableCheckpointScore(enableCheckpointScore);
        //변경된 설정 반환
        return current();
    }
}
