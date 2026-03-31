package com.example.rungame.event.service.rules;

import com.example.rungame.event.domain.EventType;
import com.example.rungame.event.domain.SessionEvent;
import com.example.rungame.event.repository.SessionEventRepository;
import com.example.rungame.event.service.EventRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/*
* 스프린트 흐름을 검증하는 룰 클래스
* - 스프린트가
*   1) 이미 달리는 중인데 다시 시작되지 않는지
* ` 2) 끝나자마자 너무 빨리 다시 시작하지는 않는지
*   3) start 없이 end만 들어오지는 않는지
*   4) 너무 짧게 켰다 껐다 하는 이상한 스프린트는 아닌지
*  를 확인함
* */
@Component
@RequiredArgsConstructor
public class SprintRule implements EventRule {

    private final SessionEventRepository repository;

    //최소 스프린트 유지 시간
    private static final int MIN_DURATION_MS = 300;
    //스프린트 종료 후 다시 시작까지 필요한 최소 쿨다운
    private static final int RESTART_COOLDOWN_MS = 200;

    //이 룰이 어떤 이벤트 타입을 대상으로 하는지 표시
    //sprint_start, sprint_end 둘 다 대상
    @Override
    public boolean supports(EventType type){
        return type == EventType.sprint_start || type == EventType.sprint_end;
    }

    /*
    * sprint_start/ sprint_end 공통 진입점
    * - 타입에 따라 1)시작 검증, 2)종료 검증을 나눠서 진행
    * */
    @Override
    public void validate(SessionEvent ev, Map<EventType, Integer> lastTms){

        EventType type = ev.getType();
        int tMs = ev.getTMs();
        Long sid = ev.getSessionId();

        //1) sprint_start 검증
        if (type == EventType.sprint_start) {

            //현재 세션에서 마지막 sprint_start/sprint_end 시점을 조회
            var lastStartOpt =
                    repository.findLastBySessionIdAndType(sid, EventType.sprint_start);
            var lastEndOpt =
                    repository.findLastBySessionIdAndType(sid, EventType.sprint_end);

            int lastStartT = lastStartOpt.map(SessionEvent::getTMs).orElse(0);
            int lastEndT   = lastEndOpt.map(SessionEvent::getTMs).orElse(0);

            //이미 스프린트 중이면 중복 start → 드랍
            if (lastStartT > lastEndT) {
                return;
            }

            //직전 sprint_end 이후 쿨다운보다 빨리 다시 시작 -> 드랍
            if (lastEndT > 0 && (tMs - lastEndT) < RESTART_COOLDOWN_MS) {
                return;
            }
        }

        //2) sprint_end 검증
        if (type == EventType.sprint_end) {

            //가장 최근 sprint_start가 있는지 확인
            var lastStartOpt =
                    repository.findLastBySessionIdAndType(sid, EventType.sprint_start);

            //start 없이 end만 들어온 경우 -> 드랍
            if (lastStartOpt.isEmpty()) {
                return;
            }

            int startT = lastStartOpt.get().getTMs();

            //시간 역행 -> 드랍
            if (tMs <= startT) {
                return;
            }

            int duration = tMs - startT;

            // 너무 짧은 sprint → 드랍
            if (duration < MIN_DURATION_MS) {
                return;
            }
        }
    }
}