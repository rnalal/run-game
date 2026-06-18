package com.example.rungame.event.service;

import com.example.rungame.event.domain.EventType;
import com.example.rungame.event.domain.SessionEvent;

import java.util.Collections;
import java.util.Map;

//이벤트 검증 룰을 위한 공통 인터페이스
public interface EventRule {

    //이 룰이 어떤 타입의 이벤트를 다루는지 여부를 반환
    boolean supports(EventType type);

    //들어온 이벤트 한 건에 대한 검증 로직
    void validate(SessionEvent event, Map<EventType, Integer> lastTms);
}
