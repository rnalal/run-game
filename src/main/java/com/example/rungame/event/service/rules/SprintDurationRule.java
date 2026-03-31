package com.example.rungame.event.service.rules;

import com.example.rungame.event.domain.EventType;
import com.example.rungame.event.domain.SessionEvent;
import com.example.rungame.event.repository.SessionEventRepository;
import com.example.rungame.event.service.EventRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/*
* 스프린트 중 sprint_end 시점에서 지속시간을 확인하는 규칙 클래스
* - 스프린트가 너무 오래 유지되는 상황을 막는 역할
*
* - sprint_end가 들어왔을 때, 최근 sprint_start 시점을 찾음
*   -> 우선 배치 컨텍스트에서 찾고 없으면 DB에서 마지막 sprint_start 한 건 조회
* - start 없이 end만 들어온 경우는 별도 처리 없이 패스
* - start ~ end 사이 시간이 MAX_SPRINT_MS를 넘으면 비정상으로 보고 버림
* */
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

    /*
    * sprint_end 이벤트 한 건에 대한 검증 로직
    * - 최근 sprint_start 시점을 기준으로 지속 시간 계산
    * - 최대 허용 시간을 넘으면 비정상으로 보고 조용히 무시
    *
    * @param e : 현재 들어온 sprint_end 이벤트
    * @param lastTms : 배치 안에서 타입별 마지막 tMs를 관리하는 맵
    * */
    @Override
    public void validate(SessionEvent e, Map<EventType, Integer> lastTms){
        //1) 배치 컨텍스트에서 직전 sprint_start tMs를 우선 사용
        Integer startT = lastTms.get(EventType.sprint_start);

        //2) 배치 컨텍스트에 없으면 DB에서 마지막 sprint_start 한 건만 조회
        if(startT == null){
            var lastStartOpt = repo.findLastBySessionIdAndType(e.getSessionId(), EventType.sprint_start);

            //start없이 end만 온 경우는 그냥 패스
            if(lastStartOpt.isEmpty()) {
                return;
            }
            startT = lastStartOpt.get().getTMs();
        }

        int duration = e.getTMs() - startT;

        //3) 비정상적으로 긴 sprint 방어 -> 드랍
        if(duration > MAX_SPRINT_MS){
            return;
        }
    }
}
