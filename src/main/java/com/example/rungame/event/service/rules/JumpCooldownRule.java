package com.example.rungame.event.service.rules;

import com.example.rungame.event.domain.EventType;
import com.example.rungame.event.domain.SessionEvent;
import com.example.rungame.event.repository.SessionEventRepository;
import com.example.rungame.event.service.EventRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

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

    //jump 이벤트 한 건에 대한 쿨다운 검증
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
