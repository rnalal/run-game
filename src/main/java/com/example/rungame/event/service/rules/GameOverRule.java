package com.example.rungame.event.service.rules;

import com.example.rungame.event.domain.EventType;
import com.example.rungame.event.domain.SessionEvent;
import com.example.rungame.event.repository.SessionEventRepository;
import com.example.rungame.event.service.EventRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/*
* gameover 이벤트 검증을 담당하는 규칙 클래스
* - 게임이 끝났다는 신호가 말이 되는 타이밍에 한 번만 들어오도록 확인하는 역할
*
* 체크
* - 아직 game_start도 안 된 세션에서 game_over가 먼저 들어오지 않았는지
* - 같은 세션에서 game_over가 여러 번 들어오지 않았는지
* */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameOverRule implements EventRule {

    /*
    * 세션 이베늩 조회용 레포지토리
    * - 해당 세션에 game_start가 있었는지 확인할 때 사용
    * */
    private final SessionEventRepository sessionEventRepository;

    /*
    * 이 규칙이 어떤 이벤트 타입을 대상으로 하는지 표시
    * - game_over 이벤트에만 적용
    * */
    @Override
    public boolean supports(EventType type){
        return type == EventType.game_over;
    }

    /*
    * game_over 이벤트 한 건에 대한 검증 로직
    * - 1)game_start보다 먼저 오는 game_over 막기
    * - 2)한 세션에서 gmae_over가 여러 번 오는 상황 막기
    * */
    @Override
    public void validate(SessionEvent incoming, Map<EventType, Integer> lastTms){
        final long sid = incoming.getSessionId();
        final int tMs = incoming.getTMs();

        //1)game_start 이전 game_over 방지
        boolean started = sessionEventRepository
                .existsBySessionIdAndType(sid, EventType.game_start);

        if (!started) {
            log.warn("[drop:game_over] before game_start sid={} tMs={}", sid, tMs);
            return;
        }

        //중복 game_over 방지
        boolean alreadyOver = lastTms.containsKey(EventType.game_over);

        if (alreadyOver) {
            log.warn("[drop:game_over] duplicate game_over sid={} tMs={}", sid, tMs);
            return;
        }

    }
}
