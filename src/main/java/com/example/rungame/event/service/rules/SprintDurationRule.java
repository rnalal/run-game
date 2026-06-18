package com.example.rungame.event.service.rules;

import com.example.rungame.event.domain.EventType;
import com.example.rungame.event.domain.SessionEvent;
import com.example.rungame.event.repository.SessionEventRepository;
import com.example.rungame.event.service.EventRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

//스프린트 중 sprint_end 시점에서 지속시간을 확인하는 규칙 클래스
@Component
@RequiredArgsConstructor
public class SprintDurationRule implements EventRule {

    private final SessionEventRepository repo;

    //최대 허용 스프린트 지속시간
    private static final int MAX_SPRINT_MS = 5000;

    //이 룰이 어떤 이벤트 타입을 대상으로 하는지 표시
    //sprint_end 이벤트에만 적용
    @Override
    public boolean supports(EventType type){
        return type == EventType.sprint_end;
    }

    //sprint_end 이벤트 한 건에 대한 검증 로직
    @Override
    public void validate(SessionEvent e, Map<EventType, Integer> lastTms){
        //배치 컨텍스트에서 직전 sprint_start tMs를 우선 사용
        Integer startT = lastTms.get(EventType.sprint_start);

        //배치 컨텍스트에 없으면 DB에서 마지막 sprint_start 한 건만 조회
        if(startT == null){
            var lastStartOpt = repo.findLastBySessionIdAndType(e.getSessionId(), EventType.sprint_start);

            //start없이 end만 온 경우는 그냥 패스
            if(lastStartOpt.isEmpty()) {
                return;
            }
            startT = lastStartOpt.get().getTMs();
        }

        int duration = e.getTMs() - startT;

        //비정상적으로 긴 sprint 방어 -> 드랍
        if(duration > MAX_SPRINT_MS){
            return;
        }
    }
}
