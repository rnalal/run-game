package com.example.rungame.event.service;

import com.example.rungame.common.support.Payloads;
import com.example.rungame.event.domain.EventType;
import com.example.rungame.event.domain.SessionEvent;
import com.example.rungame.event.dto.EventBatchRequest;
import com.example.rungame.event.dto.EventDTO;
import com.example.rungame.event.repository.SessionEventRepository;
import com.example.rungame.session.domain.Session;
import com.example.rungame.session.repository.SessionRepository;
import com.example.rungame.session.service.SessionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.*;

/*
* 클라이언트에서 올라온 게임 이벤트 배치를 받아서
* 세션 검증 -> 규칙 적용 -> 이동/점수 반영 -> 이벤트 저장까지 처리하는 핵심 서비스
* (한 세션 동안 발생한 이벤트 스트림을 검증하고 세션 상태와 점수에 반영해서 저장)
*
* 1) 세션 유효성 검증
* - 존재하는 세션인지
* - 요청한 유저의 세션이 맞는지
* - 이미 종료된 세션은 아닌지
* 2) 이벤트 배치 정리
* - EventBatchRequest 내부 이벤트들을 seq 기준으로 정렬
* - 이미 처리한 seq는 무시
* 3) 배치 컨텍스트 준비
* - lastTms: 타입별 마지막 이벤트 시각을 메모리에 올려놓고 규칙들이 활용할 수 있도록 제공
* - MovementScorer: 이동/스코어 계산을 위해 현재 이동 상태를 복원
* - Score_X2 파워업 지속 시간 복원
* 4) 이벤트 처리 루프
* - 각 EventDTO를 SessionEvent로 변환
* - 이동 거리/ 점수 정산 -> MovementScorer
* - 통과 시 DB 저장, lastTms 갱신, 이동 상태 갱신
* - game_over 수용 시 세션 종료 처리
* 5) 결과 반환
* - 실제로 저장에 성공한 이벤트 개수 반환
* */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventIngestService {

    //세션 존재 여부, 소유자, 종료 여부 등을 확인하기 위한 리포지토리
    private final SessionRepository sessionRepository;
    //세션에 속한 이벤트를 조회/저장하는 리포지토리
    private final SessionEventRepository sessionEventRepository;
    /*
    * 이벤트별 검증 규칙 목록
    * - CheckpointRule, CoinPickupRule, GameOverRule, JumpCooldowmRule 등
    *   EventRule을 구현한 여러 규칙들이 주입됨
    * - 이벤트 타입별로 supports을 보고 필요한 규칙들만 실행
    * */
    private final List<EventRule> rules;
    //세션 종료 처리 등 세션 라이프사이클을 담당하는 서비스
    private final SessionService sessionService;

    /*
    * 이벤트 배치 수집 진입점
    * - 한 유저의 한 세션에 대해 여러 이벤트를 한 번에 받아 처리함
    * 1) 세션 검증(존재/소유자/종료 여부)
    * 2) 기존 마지막 seq 조회 후 새 배치 이벤트를 seq 오름차순 정렬
    * 3) lastTms/ MovementScorer/ SCORE_X2 상태 초기화
    * 4) 이벤트 루프
    *   - 이동 거리/점수 정산
    *   - 규칙 실행
    *   - 유효하면 SessionEvent 저장 + 이동 상태 갱신
    *   - game_over 수용 시 이후 이벤트는 무시
    * 5) game_over가 수용된 경우 세션 종료 처리
    *
    * @param userId : 요청을 보낸 유저 ID
    * @param sessionId : 이벤트가 속한 세션 ID
    * @param req : 클라이언트에서 올라온 이벤트 배치 요청
    * @return : 실제로 저장에 성공한 이벤트 개수
    * */
    @Transactional
    public int ingest(Long userId, Long sessionId, EventBatchRequest req) {

        //1. 세션 기본 검증
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

        //2. seq 기준 정렬
        int lastSeq = sessionEventRepository
                .findTopBySessionIdOrderBySeqDesc(sessionId)
                .map(SessionEvent::getSeq)
                .orElse(-1);

        //배치로 들어온 이벤트들을 새 리스트에 복사 후 seq 오름차순 정렬
        List<EventDTO> sorted = new ArrayList<>(req.getEvents());
        sorted.sort(Comparator.comparingInt(EventDTO::getSeq));

        //3. 배치 컨텍스트 (핵심)
        //타입별 마지막 tMs를 메모리에 올려두고 각 EventRule이 참고할 수 있도록 제공
        Map<EventType, Integer> lastTms = new EnumMap<>(EventType.class);
        //이전에 저장된 이벤트 기준으로 lastTms를 초기화
        seedLastTms(sessionId, lastTms);

        //4. 이동 스코어러 준비
        //이동 계산 기준 시간 설정: 기준 이벤트 중 가장 늦은 tMs
        int movementAnchor = lastTms.values().stream().max(Integer::compareTo).orElse(0);
        MovementScorer mover = new MovementScorer(movementAnchor);

        //스프린트 등 이동 상태를 기존 세션 상태에서 복원
        seedMovementState(sessionId, mover);

        //5. SCORE_X2 시드
        //마지막 SCORE_X2 파워업 기준으로 몇 ms까지 2배 점수인지 복원
        int scoreX2UntilMs = seedScoreX2(sessionId);

        //실제 저장된 이벤트 수
        int accepted = 0;
        //game_over가 처리됐는지 여부
        boolean gameOverAccepted = false;

        //6. 이벤트 처리 루프
        for (EventDTO dto : sorted) {

            // 이미 game_over 이벤트를 수용한 이후의 이벤트는 전부 무시
            if (gameOverAccepted) {
                break;
            }

            //기존에 처리한 seq 이하면 무시
            if (dto.getSeq() <= lastSeq) continue;
            //tMs가 0 이하인 비정상 이벤트도 무시
            if (dto.getTMs() <= 0) continue;

            //이동 구간 정산
            try {
                //현재 이벤트 시각까지의 이동 거리/점수 누적
                MovementScorer.Result move = mover.flushUntil(dto.getTMs(), scoreX2UntilMs);
                if (move.distPx > 0 || move.scoreDelta > 0) {
                    sessionRepository.incrDistanceAndScore(
                            sessionId,
                            (int) move.distPx,
                            move.scoreDelta
                    );
                }
            } catch (Exception ex) {
                //이동 계산 중 오류가 나도 게임 진행이 막히지 않도록 방어
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

            //저장
            try {
                sessionEventRepository.save(ev);

                accepted++;
                lastSeq = ev.getSeq();
                lastTms.put(ev.getType(), ev.getTMs());

                //스프린트/슬라이드/역주행/일시정지 등 이동 상태 반영
                applyMovementSwitch(ev, mover);

                //game_over 이벤트를 수용하면 이후 이벤트는 더 처리하지 않음
                if (ev.getType() == EventType.game_over) {
                    gameOverAccepted = true;
                }

            } catch (DataIntegrityViolationException dup) {
                //같은 seq로 이미 저장된 이벤트가 있는 경우 무시
                log.debug("[dup] sid={} seq={}", sessionId, ev.getSeq());
            }
        }

        //7. game_over 처리
        if (gameOverAccepted) {
            //세션 종료 시각 설정/ 마지막 정산 등 세션 마무리
            sessionService.endSession(userId, sessionId, null);
        }

        log.debug("[ingest:done] sid={} accepted={}", sessionId, accepted);
        return accepted;
    }

    /*
    * lastTms 초기화
    * - 기존에 저장된 SessionEvent들을 기준으로 타입별 마지막 tMs를 lastTms 맵에 채워 넣음
    * - 이후 EventRule들이 이 타입은 마지막에 언제 들어왔는지를 참고하는 데 사용
    * */
    private void seedLastTms(Long sessionId, Map<EventType, Integer> lastTms) {
        for (EventType type : EventType.values()) {
            sessionEventRepository.findLastBySessionIdAndType(sessionId, type)
                    .filter(ev -> ev.getTMs() > 0)
                    .ifPresent(ev -> lastTms.put(type, ev.getTMs()));
        }
    }

    /*
    * MovementScorer 초기 상태 세팅
    * - 세션의 마지막 sprint_start / sprint_end를 기준으로 현재 스프린트 중인지 여부를 복원해줌
    * - 향후 jump/slide/reverse 등 다른 상태들도 확장 가능
    * */
    private void seedMovementState(Long sessionId, MovementScorer mover) {
        boolean sprint =
                sessionEventRepository.findLastBySessionIdAndType(sessionId, EventType.sprint_start)
                        .map(s -> sessionEventRepository
                                .findLastBySessionIdAndType(sessionId, EventType.sprint_end)
                                .map(e -> s.getTMs() > e.getTMs())
                                .orElse(true))
                        .orElse(false);

        mover.setSprint(sprint);
    }

    /*
    * SCORE_X2 파워업 상태 초기화
    * - 마지막 powerup_pick 이벤트를 조회 -> 이름이 SCORE_X2인지 확인 -> 해당 지속시간만큼 tMs에 더해
    *   언제까지 2배 점수인지를 계산
    *
    * @return SCORE_X2 : 효과가 끝나는 시각. 없으면 -1
    * */
    private int seedScoreX2(Long sessionId) {
        return sessionEventRepository.findLastBySessionIdAndType(sessionId, EventType.powerup_pick)
                .map(ev -> {
                    String name = Payloads.getString(ev.getPayload(), "name", "");
                    if (!"SCORE_X2".equals(name)) return -1;
                    int dur = Payloads.getInt(ev.getPayload(), "durationMs", 5000);
                    return ev.getTMs() + dur;
                })
                .orElse(-1);
    }

    /*
    * 개별 이벤트에 따라 MovementScorer의 상태를 전환
    * - sprint/reverse/slide/pause 등 이동 속도나 방향, 정지 여부에 영향을 주는 이벤트들을
    *   MovementScorer 내부 상태로 반영함
    * */
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
