package com.example.rungame.event.service.rules;

import com.example.rungame.event.domain.EventType;
import com.example.rungame.event.domain.SessionEvent;
import com.example.rungame.event.repository.SessionEventRepository;
import com.example.rungame.event.service.EventRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/*
* 점프 이벤트 쿨다운을 담당하는 규칙 클래스
* - 플레이어가 너무 짧은 간격으로 연속 점프하는 상황을 막기 위한 룰
* - 최소 간격을 기준으로 그보다 빠른 jump 이벤트는 조용히 무시
*
* - 같은 배치 안에서 마지막 jump 시각 확인
* - 없다면 DB에서 마지막 jump를 찾아와 기준값으로 사용
* - 현재 jump와의 시간 차이가 쿨다운 이상인지 확인
* */
@Component
@RequiredArgsConstructor
@Slf4j
public class JumpCooldownRule implements EventRule {

    private final SessionEventRepository repo;
    //점프 사이 최소 허용 간격
    private static final int MIN_COOLDOWN_MS = 450;

    //어떤 타입의 이벤트에 이 룰을 적용할지 명시
    //jump 이벤트에만 적용
    @Override
    public boolean supports(EventType type) {
        return type == EventType.jump;
    }

    /*
    * jump 이벤트 한 건에 대한 쿨다운 검증
    * - 배치 컨텍스트에 있는 마지막 jump 시간 우선 활용
    * - 없다면 DB에서 마지막 jump를 가져와 보정
    * - 최소 쿨다운을 어기면 로그만 남기고 무시
    *
    * @param e : 현재 들어온 jump 이벤트
    * @param lastTms : 배치 안에서 타입별 마지막 tMs를 관리하는 맵
    * */
    @Override
    public void validate(SessionEvent e, Map<EventType, Integer> lastTms){

        //배치 기준 마지막 jump 시각
        Integer lastJumpT = lastTms.get(EventType.jump);

        //배치 컨텍스트에 정보가 없으면 DB에서 마지막 jump를 한 번 찾아와 채워줌
        if(lastJumpT == null) {
            repo.findLastBySessionIdAndType(e.getSessionId(), EventType.jump)
                    .ifPresent(last -> lastTms.put(EventType.jump, last.getTMs()));
            lastJumpT = lastTms.get(EventType.jump);
        }

        //마지막 jump가 없으면 이번 점프는 첫 점프로 간주 -> 그냥 허용
        if(lastJumpT == null){
            log.debug("[jump] first jump OK (sid={} , tMs={})", e.getSessionId(), e.getTMs());
            return; // 첫 점프 허용
        }

        int diff = e.getTMs() - lastJumpT;
        log.debug("[jump] sid={} lastJumpT={} curr={} diff={}", e.getSessionId(), lastJumpT, e.getTMs(), diff);

        // 쿨다운 미만 -> 조용히 무시
        if(diff < MIN_COOLDOWN_MS) {
            log.debug("[jump] cooldown violated → ignored (sid={}, diff={})",
                    e.getSessionId(), diff);
            return;
        }
    }
}
