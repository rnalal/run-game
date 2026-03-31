package com.example.rungame.event.service;

import com.example.rungame.event.domain.EventType;
import com.example.rungame.event.domain.SessionEvent;

import java.util.Collections;
import java.util.Map;

/*
* 이벤트 검증 룰을 위한 공통 인터페이스
* - 각 이벤트를 받아들일지 말지를 결정하는 규칙을 만들 때
*   이 인터페이스를 기준으로 구현
*
* 1) supports(EventType type)
*   - 나는 어떤 타입의 이벤트를 다루는 룰이다를 알려주는 메서드
* 2) validate(SessionEvent event, Map<EventType, Integer> lastTms)
*   - 실제 검증 로직을 담는 메서드
*   - 들어온 이벤트가 정상인지 비정상이라면 버려야 하는지 판단에 사용
*   - 두 번째 인자인 lastTms는 타입별 마지막 이베늩 시각을 담고 있는 배치 컨텍스트
*     (key: EventType/ value: 마지막 tMs)
*
* EventIngestService와의 관계
* - EventIngestService는 이벤트 배치를 받으면
*   1) SessionEvent로 변환한 뒤
*   2) 등록된 EventRule 리스트를 돌면서
*      supports이 true인 룰들만 골라 validate를 호출
*   3) 검증 중 문제가 생기거나 룰에서 비정상으로 판단되면 이벤트를 저장하지 않고 건너뜀
* */
public interface EventRule {
    /*
    * 이 룰이 어떤 타입의 이벤트를 다루는지 여부를 반환
    * ex) coin_pick 전용 룰이라면 type==EventType.coin_pick일때만 true
    *
    * @param type : 이벤트 타입
    * @return : 이 타입을 처리 대상으로 삼는다면 true, 아니면 false
    * */
    boolean supports(EventType type);

    /*
    * 들어온 이벤트 한 건에 대한 검증 로직
    * - event : 현재 검증 중인 이벤트 (세션 ID, seq, tMs, type, payload 포함)
    * - lastTms : 타입별 마지막 tMs를 담고 있는 배치 컨텍스트 -> 이 타입은 방금 전까지 언제 들어왔는지를 참고
    *
    * ex) jump 쿨다운 체크, game_over가 game_start 이후에 한 번만 오는지 확인
    * */
    void validate(SessionEvent event, Map<EventType, Integer> lastTms);
}
