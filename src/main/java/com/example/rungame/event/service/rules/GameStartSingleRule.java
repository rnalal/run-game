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

/*
* game_start 이벤트가 한 번만 처리되도록 체크하는 룰
* - 한 번의 배치 안에서 game_start가 여러 번 들어오는 상황을 막는 역할
* - DB 조회보다는 우선 배치 컨텍스트lastTms를 기준으로 판단
* - 한 번의 이벤트 전송 안에서 중복 호출을 막는거
* */
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

    /*
    * game_start 이벤트 한 건에 대한 검증 로직
    * - 같은 배치 안에서 이미 game_start가 처리된 적 있다면
    *   이번 game_start는 중복으로 보고 로그만 남기고 무시
    * @param e : 현재 들어온 game_start 이벤트
    * @param lastTms : 배치 안에서 타입별 마지막 tMs를 관리하는 맵
    * */
    @Override
    public void validate(SessionEvent e, Map<EventType, Integer> lastTms){

        //배치 컨텍스트에 이미 game_start가 있다면 그냥 무시
        if(lastTms.containsKey(EventType.game_start)){
            log.debug("[drop:game_start] duplicate in batch sid={}", e.getSessionId());
            return;
        }
    }
}
