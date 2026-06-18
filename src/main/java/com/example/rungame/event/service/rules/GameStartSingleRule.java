package com.example.rungame.event.service.rules;

import com.example.rungame.event.domain.EventType;
import com.example.rungame.event.domain.SessionEvent;
import com.example.rungame.event.repository.SessionEventRepository;
import com.example.rungame.event.service.EventRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.events.Event;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameStartSingleRule implements EventRule {

    private final SessionEventRepository repo;

    //어떤 타입의 이벤트에 이 룰을 적용할지 명시
    //game_start 이벤트만 대상
    @Override
    public boolean supports(EventType type){
        return type == EventType.game_start;
    }

    //game_start 이벤트 한 건에 대한 검증 로직
    @Override
    public void validate(SessionEvent e, Map<EventType, Integer> lastTms){

        //배치 컨텍스트에 이미 game_start가 있다면 그냥 무시
        if(lastTms.containsKey(EventType.game_start)){
            log.debug("[drop:game_start] duplicate in batch sid={}", e.getSessionId());
            return;
        }
    }
}
