package com.example.rungame.event.service.rules;

import com.example.rungame.event.domain.EventType;
import com.example.rungame.event.domain.SessionEvent;
import com.example.rungame.event.repository.SessionEventRepository;
import com.example.rungame.event.service.EventRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameOverRule implements EventRule {

    private final SessionEventRepository sessionEventRepository;

    //이 규칙이 어떤 이벤트 타입을 대상으로 하는지 표시
    @Override
    public boolean supports(EventType type){
        return type == EventType.game_over;
    }

    //game_over 이벤트 한 건에 대한 검증 로직
    @Override
    public void validate(SessionEvent incoming, Map<EventType, Integer> lastTms){
        final long sid = incoming.getSessionId();
        final int tMs = incoming.getTMs();

        //game_start 이전 game_over 방지
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
