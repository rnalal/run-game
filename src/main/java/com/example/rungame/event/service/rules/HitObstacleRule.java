package com.example.rungame.event.service.rules;

import com.example.rungame.event.domain.EventType;
import com.example.rungame.event.domain.SessionEvent;
import com.example.rungame.event.repository.SessionEventRepository;
import com.example.rungame.event.service.EventRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

//장애물 충돌 이벤트 검증을 담당하는 규칙 클래스
@Component
@RequiredArgsConstructor
public class HitObstacleRule implements EventRule {

    private final SessionEventRepository sessionEventRepository;

    //최소 간격 - 너무 자주 부딪히는 패턴 방지용
    private static final int MIN_COOLDOWN_MS = 300;

    //이 규칙이 어떤 이벤트 타입에 적용되는지 표시
    //hit_obstacle 이벤트에만 적용
    @Override
    public boolean supports(EventType type) {
        return type == EventType.hit_obstacle;
    }

    //hit_obstacle 이벤트 한 건에 대한 검증 로직
    @Override
    public void validate(SessionEvent incoming, Map<EventType, Integer> lastTms){

        //game_start 이후에만 충돌 이벤트를 인정
        //- 아직 게임이 시작도 안 했는데 충돌했다고 오는 경우는 무시
        if (!lastTms.containsKey(EventType.game_start)) return;

        //최소 쿨다운 체크
        Integer last = lastTms.get(EventType.hit_obstacle);
        if (last == null) {
            //배치 컨텍스트에 정보가 없으면
            //DB에서 마지막 hib_obstacle을 찾아솨 시드로 사용
            sessionEventRepository.findLastBySessionIdAndType(incoming.getSessionId(), EventType.hit_obstacle)
                    .map(SessionEvent::getTMs).ifPresent(t -> lastTms.put(EventType.hit_obstacle, t));
            last = lastTms.get(EventType.hit_obstacle);
        }

        //마지막 충돌 시각이 있고 이번 충돌까지의 간격이 너무 짧다면 쿨다운 미달로 판단
        if(last != null && incoming.getTMs() - last < MIN_COOLDOWN_MS){
            return;
        }

    }
}
