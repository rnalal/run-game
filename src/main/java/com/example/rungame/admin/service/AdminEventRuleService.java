package com.example.rungame.admin.service;

import com.example.rungame.event.service.RuleRuntimeConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminEventRuleService {

    //이벤트 룰 런타임 설정 객체
    private final RuleRuntimeConfig cfg;

    //현재 이벤트 룰 조회
    public Map<String, Object> current() {
        return Map.of(
                "jumpMinIntervalMs", cfg.getJumpMinIntervalMs(),
                "sprintMinDurationMs", cfg.getSprintMinDurationMs(),
                "reverseMaxDurationMs", cfg.getReverseMaxDurationMs(),
                "enableCheckpointScore", cfg.isEnableCheckpointScore()
        );
    }

    //이벤트 룰 업데이트
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
