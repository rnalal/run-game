package com.example.rungame.event.service.rules;

import com.example.rungame.event.domain.EventType;
import com.example.rungame.event.domain.SessionEvent;
import com.example.rungame.event.repository.SessionEventRepository;
import com.example.rungame.event.service.EventRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/*
* 슬라이드 이벤트를 검증하는 규칙 클래스
*
* -슬라이드가
*  1) 게임이 시작된 이후에만 일어나는지
*  2) 슬라이드 중복 시작은 없는지
*  3) 슬라이드 사이에 쿨다운을 지키는지
*  4) 슬라이드 시간이 너무 짧거나 너무 길지는 않은지
*  를 확인하는 역할
*
* - slide_start, slide_end를 한 쌍으로 보고
*   열림 슬라이드가 있을 때만 end를 인정하는 흐름
* - 최소/최대 슬라이드 시간, 쿨다운을 코드로 명시해 게임 플레이 템포를 유지
* */
@Component
@RequiredArgsConstructor
public class SlideRule implements EventRule {

    private final SessionEventRepository sessionEventRepository;

    //슬라이드 유지 시간 최소/최대 및 쿨다운
    //너무 짧은 슬라이드는 비정상으로 판단
    private static final int MIN_SLIDE_MS = 200;
    //너무 긴 슬라이드는 비정상으로 판단
    private static final int MAX_SLIDE_MS = 3000;
    //슬라이드 종료 후 다시 시작까지 최소 간격
    private static final int SLIDE_COOLDOWN_MS = 300;

    //slide_start/ slide_end 두 타입 모두를 대상으로 하는 룰
    @Override
    public boolean supports(EventType type) {
        return type == EventType.slide_start || type == EventType.slide_end;
    }

    /*
    * 슬라이드 관련 이벤트 한 건에 대한 검증 진입점
    * - 실제 검증은 start/end 각각에 맞는 메서드에서 처리
    * */
    @Override
    public void validate(SessionEvent incoming, Map<EventType, Integer> lastTms){
        if (incoming.getType() == EventType.slide_start){
            validateStart(incoming, lastTms);
        } else if (incoming.getType() == EventType.slide_end){
            validateEnd(incoming, lastTms);
        }
    }

    /*
    * slide_start 검증
    * - game_start가 먼저 있었는지
    * - 이미 슬라이드 중인 상태는 아닌지
    * - 마지막 slide_end 이후 쿨다운을 지켰는지
    * */
    private void validateStart(SessionEvent incoming, Map<EventType, Integer> lastTms){
        //1) game_start 이후만 허용
        Integer gs = lastTms.get(EventType.game_start);
        if (gs == null) return;

        //2) 이미 슬라이딩 중인지 확인 - 이미 슬라이드 중이면 새 start는 드랍
        Integer lastStart = lastTms.get(EventType.slide_start);
        Integer lastEnd   = lastTms.get(EventType.slide_end);

        // 컨텍스트에 없다면 DB에서 1회만 초기값 가져오기
        if (lastStart == null) {
            sessionEventRepository.findLastBySessionIdAndType(incoming.getSessionId(), EventType.slide_start)
                    .ifPresent(ev -> lastTms.put(EventType.slide_start, ev.getTMs()));
            lastStart = lastTms.get(EventType.slide_start);
        }
        if (lastEnd == null) {
            sessionEventRepository.findLastBySessionIdAndType(incoming.getSessionId(), EventType.slide_end)
                    .ifPresent(ev -> lastTms.put(EventType.slide_end, ev.getTMs()));
            lastEnd = lastTms.get(EventType.slide_end);
        }

        //lastStart는 있는데 lastEnd가 없거나 lastStart > lastEnd이면 아직 슬라이드 중이라고 판단
        boolean slidingNow = (lastStart != null) && (lastEnd == null || lastStart > lastEnd);
        //중복 slide_start는 무시
        if (slidingNow) return;

        // 3) 쿨다운: 마지막 slide_end 기준
        if(lastEnd != null && incoming.getTMs() - lastEnd < SLIDE_COOLDOWN_MS){
            //쿨다운 미만이면 새 슬라이드 시작은 허용하지 않음
            return;
        }
    }

    /*
    * slide_end 검증
    * - 최근 slide_start가 있는지
    * - 이미 종료된 슬라이드가 아닌지
    * - 시간 순서가 올바른지
    * - 슬라이드 지속 시간이 정상 범위 내인지
    * */
    private void validateEnd(SessionEvent incoming, Map<EventType, Integer> lastTms) {
        //컨텍스트에서 start/end 정보 가져오기
        Integer lastStart = lastTms.get(EventType.slide_start);
        Integer lastEnd   = lastTms.get(EventType.slide_end);

        //컨텍스트에 없으면 DB에서 1회 초기값 가져오기
        if (lastStart == null) {
            sessionEventRepository.findLastBySessionIdAndType(incoming.getSessionId(), EventType.slide_start)
                    .ifPresent(ev -> lastTms.put(EventType.slide_start, ev.getTMs()));
            lastStart = lastTms.get(EventType.slide_start);
        }
        if (lastEnd == null) {
            sessionEventRepository.findLastBySessionIdAndType(incoming.getSessionId(), EventType.slide_end)
                    .ifPresent(ev -> lastTms.put(EventType.slide_end, ev.getTMs()));
            lastEnd = lastTms.get(EventType.slide_end);
        }

        //start 없으면 end는 의미 없음 -> 드랍
        if (lastStart == null) return;

        //이미 닫힌 상태라면 다시 end를 받지 않음
        if (lastEnd != null && lastEnd > lastStart) return;

        //시간 순서가 이상하면 드랍
        if (incoming.getTMs() <= lastStart) return;

        //슬라이드 지속 시간이 정상 범위 안에 있는지 확인
        int duration = incoming.getTMs() - lastStart;
        if (duration < MIN_SLIDE_MS || duration > MAX_SLIDE_MS) return;
    }

}
