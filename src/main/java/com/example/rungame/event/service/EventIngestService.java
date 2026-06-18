package com.example.rungame.event.service;

import com.example.rungame.common.support.Payloads;
import com.example.rungame.event.domain.EventType;
import com.example.rungame.event.domain.SessionEvent;
import com.example.rungame.event.dto.EventBatchRequest;
import com.example.rungame.event.dto.EventDTO;
import com.example.rungame.event.repository.SessionEventJdbcRepository;
import com.example.rungame.event.repository.SessionEventRepository;
import com.example.rungame.session.domain.Session;
import com.example.rungame.session.repository.SessionRepository;
import com.example.rungame.session.service.SessionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;

//클라이언트에서 올라온 게임 이벤트 배치를 받아서
//세션 검증 -> 규칙 적용 -> 이동/점수 반영 -> 이벤트 저장까지 처리하는 서비스
@Service
@RequiredArgsConstructor
@Slf4j
public class EventIngestService {

    private static final int STATE_RESTORE_EVENT_LIMIT = 100;

    private final SessionRepository sessionRepository;
    private final SessionEventJdbcRepository sessionEventJdbcRepository;
    private final SessionEventRepository sessionEventRepository;
    //이벤트별 검증 규칙 목록
    private final List<EventRule> rules;
    //세션 종료 처리 등 세션 라이프사이클을 담당하는 서비스
    private final SessionService sessionService;

    //이벤트 배치 수집 진입점
    @Transactional
    public int ingest(Long userId, Long sessionId, EventBatchRequest req) {

        //세션 기본 검증
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("session not found"));

        //요청 유저와 세션 소유자 검증
        if (!Objects.equals(session.getUserId(), userId))
            throw new IllegalArgumentException("not your session");

        //이미 종료된 세션이면 배치 전체 무시
        if (session.getEndedAt() != null) {
            log.debug("[ingest] sid={} already ended → ignore batch", sessionId);
            return 0;
        }

        //비어 있는 배치라면 처리하지 않음
        if (req == null || req.getEvents() == null || req.getEvents().isEmpty()) {
            log.debug("[ingest] sid={} empty batch", sessionId);
            return 0;
        }

        //기존 이벤트 상태 복원
        List<SessionEvent> recentEvents = sessionEventRepository.findRecentBySessionIdOrderByTmsDescIdDesc(
                sessionId,
                PageRequest.of(0, STATE_RESTORE_EVENT_LIMIT)
        );

        int lastSeq = recentEvents.stream()
                .mapToInt(SessionEvent::getSeq)
                .max()
                .orElse(-1);

        //배치로 들어온 이벤트들을 새 리스트에 복사 후 seq 오름차순 정렬
        List<EventDTO> sorted = new ArrayList<>(req.getEvents());
        sorted.sort(Comparator.comparingInt(EventDTO::getSeq));

        //배치 컨텍스트
        Map<EventType, SessionEvent> lastEventByType = buildLastEventByType(recentEvents);

        //타입별 마지막 tMs를 메모리에 올려두고 각 EventRule이 참고할 수 있도록 제공
        Map<EventType, Integer> lastTms = new EnumMap<>(EventType.class);
        //이전에 저장된 이벤트 기준으로 lastTms를 초기화
        seedLastTms(lastEventByType, lastTms);

        //이동 스코어러 준비
        //이동 계산 기준 시간 설정: 기준 이벤트 중 가장 늦은 tMs
        int movementAnchor = lastTms.values().stream().max(Integer::compareTo).orElse(0);
        MovementScorer mover = new MovementScorer(movementAnchor);

        //스프린트 등 이동 상태를 기존 세션 상태에서 복원
        seedMovementState(lastEventByType, mover);

        //SCORE_X2 시드
        //마지막 SCORE_X2 파워업 기준으로 몇 ms까지 2배 점수인지 복원
        int scoreX2UntilMs = seedScoreX2(lastEventByType);

        //실제 저장된 이벤트 수
        int accepted = 0;
        //game_over가 처리됐는지 여부
        boolean gameOverAccepted = false;
        //검증을 통과한 이벤트들을 모아두었다가 루프 종료 후 saveAll()로 한 번에 저장
        List<SessionEvent> acceptedEvents = new ArrayList<>();

        //이벤트 처리 중 발생한 거리/점수 증가분을 루프 안에서 DB에 바로 반영하지 않고 누적
        int totalDistanceDelta = 0;
        int totalScoreDelta = 0;
        int totalCoinsDelta = 0;

        //이벤트 처리 루프
        for (EventDTO dto : sorted) {

            //이미 game_over 이벤트를 수용한 이후의 이벤트는 전부 무시
            if (gameOverAccepted) {
                break;
            }

            //기존에 처리한 seq 이하면 무시
            if (dto.getSeq() <= lastSeq) continue;
            //tMs가 0 이하인 비정상 이벤트도 무시
            if (dto.getTMs() <= 0) continue;

            //이동 구간 정산
            try {
                //현재 이벤트 시각까지의 이동 거리/점수 증가분 계산
                MovementScorer.Result move = mover.flushUntil(dto.getTMs(), scoreX2UntilMs);

                //배치 내에서 누적한 뒤 루프 종료 후 한 번만 UPDATE
                if (move.distPx > 0 || move.scoreDelta > 0) {
                    totalDistanceDelta += (int) move.distPx;
                    totalScoreDelta += move.scoreDelta;
                }

            } catch (Exception ex) {
                //이동 계산 중 오류가 나도 게임 진해잉 막히지 않도록 방어
                log.warn("[move:skip] sid={} tMs={}", sessionId, dto.getTMs());
            }

            //DTO -> 엔티티 변환
            SessionEvent ev = SessionEvent.builder()
                    .sessionId(sessionId)
                    .seq(dto.getSeq())
                    .tMs(dto.getTMs())
                    .type(dto.getType())
                    .payload(dto.getPayloadJson())
                    .build();

            //Rule 검증
            boolean valid = true;
            try {
                for (EventRule rule : rules) {
                    if (rule.supports(ev.getType())) {
                        //각 이벤트 타입을 담당하는 규칙들 실행
                        rule.validate(ev, lastTms);
                    }
                }
            } catch (Exception ex) {
                //규칙 처리 중 예외가 나면 비정상 이벤트로 간주
                valid = false;
            }

            if (!valid) continue;

            //coin_pick 이벤트는 세션 종료 시 다시 집계하지 않도록 수집 단계에서 코인/점수를 누적
            if (ev.getType() == EventType.coin_pick) {
                int coinValue = Payloads.getInt(ev.getPayload(), "value", 1);

                totalCoinsDelta += coinValue;

                boolean x2 = ev.getTMs() <= scoreX2UntilMs;
                totalScoreDelta += coinValue * (x2 ? 2 : 1);
            }

            //저장은 루프 안에서 바로 하지 않고, 검증을 통과한 이벤트를 리스트에 모아두고
            //루프 종료 후 saveAll()로 한 번에 처리
            acceptedEvents.add(ev);

            accepted++;
            lastSeq = ev.getSeq();
            lastTms.put(ev.getType(), ev.getTMs());

            //스프린트, 슬라이드, 역주행, 일시정지 등 이동 상태 반영
            applyMovementSwitch(ev, mover);

            //game_over 이벤트를 수용하면 이후 이벤트는 더 처리하지 않음
            if (ev.getType() == EventType.game_over) {
                gameOverAccepted = true;
            }
        }

        //배치 처리 중 누적된 거리/점수 증가분을 sessions 테이블에 한 번만 반영
        if (totalDistanceDelta > 0 || totalScoreDelta > 0 || totalCoinsDelta > 0) {
            long updateStart = System.currentTimeMillis();

            int updated = sessionRepository.incrDistanceScoreAndCoins(
                    sessionId,
                    totalDistanceDelta,
                    totalScoreDelta,
                    totalCoinsDelta
            );

            long updateEnd = System.currentTimeMillis();

            log.debug("[session-aggregate-update] sid={} distDelta={} scoreDelta={} coinsDelta={} updated={} elapsed={}ms",
                        sessionId,
                        totalDistanceDelta,
                        totalScoreDelta,
                        totalCoinsDelta,
                        updated,
                        updateEnd - updateStart
                    );
        }

        //검증을 통과한 이벤트들을 JDBC Batch + INSERT IGNORE로 한 번에 저장
        int inserted = 0;

        if (!acceptedEvents.isEmpty()) {
            long saveStart = System.currentTimeMillis();

            inserted = sessionEventJdbcRepository.batchInsertIgnore(acceptedEvents);

            long saveEnd = System.currentTimeMillis();

            log.debug("[event-batch-insert] sid={} valid={} inserted={} ignored={} elapsed={}ms",
                    sessionId,
                    acceptedEvents.size(),
                    inserted,
                    acceptedEvents.size() - inserted,
                    saveEnd - saveStart
            );
        }

        //game_over 처리
        if (gameOverAccepted) {
            //세션 종료 시각 설정/ 마지막 정산 등 세션 마무리
            sessionService.endSession(userId, sessionId, null);
        }

        log.debug("[ingest:done] sid={} accepted={} inserted={}", sessionId, accepted, inserted);
        return inserted;
    }

    //최근 이벤트 목록을 기반으로 타입별 마지막 이벤트
    private Map<EventType, SessionEvent> buildLastEventByType(List<SessionEvent> recentEvents) {
        Map<EventType, SessionEvent> lastEventByType = new EnumMap<>(EventType.class);

        for (SessionEvent event : recentEvents) {
            if (event.getTMs() <= 0) {
                continue;
            }
            lastEventByType.putIfAbsent(event.getType(), event);
        }
        return lastEventByType;
    }

    //EventRule 검증에서 사용할 타입별 마지막 tMs를 복원
    private void seedLastTms(
            Map<EventType, SessionEvent> lastEventByType,
            Map<EventType, Integer> lastTms
    ){
        for (Map.Entry<EventType, SessionEvent> entry : lastEventByType.entrySet()) {
            SessionEvent event = entry.getValue();

            if (event != null && event.getTMs() > 0) {
                lastTms.put(entry.getKey(), event.getTMs());
            }
        }
    }

    //MovementScorer 초기 상태 세팅
    private void seedMovementState(
            Map<EventType, SessionEvent> lastEventByType,
            MovementScorer mover
    ) {
        SessionEvent sprintStart = lastEventByType.get(EventType.sprint_start);
        SessionEvent sprintEnd = lastEventByType.get(EventType.sprint_end);

        boolean sprint = false;

        if (sprintStart != null) {
            sprint = sprintEnd == null || sprintStart.getTMs() > sprintEnd.getTMs();
        }

        mover.setSprint(sprint);
    }

    //SCORE_X2 파워업 상태 초기화
    private int seedScoreX2(Map<EventType, SessionEvent> lastEventByType) {
        SessionEvent powerup = lastEventByType.get(EventType.powerup_pick);

        if(powerup == null) {
            return -1;
        }

        String name = Payloads.getString(powerup.getPayload(), "name", "");

        if (!"SCORE_X2".equals(name)){
            return -1;
        }

        int dur = Payloads.getInt(powerup.getPayload(), "durationMs", 5000);

        return powerup.getTMs() + dur;
    }

    //개별 이벤트에 따라 MovementScorer의 상태를 전환
    private void applyMovementSwitch(SessionEvent ev, MovementScorer mover) {
        switch (ev.getType()) {
            case sprint_start -> mover.setSprint(true);
            case sprint_end -> mover.setSprint(false);
            case reverse_start -> mover.setReverse(true);
            case reverse_end -> mover.setReverse(false);
            case slide_start -> mover.setSlide(true);
            case slide_end -> mover.setSlide(false);
            case pause -> mover.setPaused(true);
            case resume -> mover.setPaused(false);
        }
    }
}
