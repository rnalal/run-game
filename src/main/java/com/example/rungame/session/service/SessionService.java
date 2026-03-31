package com.example.rungame.session.service;

import com.example.rungame.common.support.Payloads;
import com.example.rungame.event.domain.EventType;
import com.example.rungame.event.domain.SessionEvent;
import com.example.rungame.event.repository.SessionEventRepository;
import com.example.rungame.event.service.MovementScorer;
import com.example.rungame.score.domain.SessionScore;
import com.example.rungame.score.repository.SessionScoreRepository;
import com.example.rungame.session.domain.Session;
import com.example.rungame.session.dto.EndSessionRequest;
import com.example.rungame.session.dto.SessionSummaryResponse;
import com.example.rungame.session.dto.StartSessionRequest;
import com.example.rungame.session.repository.SessionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.*;

/*
* 게임 세션 관리 핵심 서비스
* - 한 판 게임에 대한 전체 생명주기를 담당하고 종료 시점에 이벤트 로그로부터 점수,거리,코인,플래그를
*   서버 기준으로 재계산하는 서비스
*
* - 세션 시작/종료 처리
*   -startSession: 새 게임 세션 생성
*   -endSession: 클라이언트 종료 요청 처리+이벤트 기반 최종 집계
*   -adminForceEnd: 관리자가 강제로 세션을 종료+재계산
* - 이벤트 기반 집계 로직
*   - aggregateSession: MovementScorer+각종 파워업,히트,체크포인트를 반영해
*     최종 점수,거리,코인,최대속도,플래그,활성시간 계산
*   - computeActiveDurationMs: pause/resume 구간을 제외한 실제 플레이 시간 계산
* - 무결성/랭킹 기록
*   -computeChecksum: 이벤트 로그 기반 세션 체크섬 계산
*   -writeScroes: 랭킹/통계용 SessionScore 레코드 저장
* */
@Service
@RequiredArgsConstructor
public class SessionService {

    //세션 상태,집계 필드를 관리하는 JPA 레포지토리
    private final SessionRepository sessionRepository;
    //세션별 이벤트 로그를 조회,저장하는 JPA 레포지토리
    private final SessionEventRepository sessionEventRepository;
    //기간별 랭킹,통계용 점수 기록을 관리하는 레포지토리
    private final SessionScoreRepository sessionScoreRepository;

    /*
    * 세션 시작
    *
    * 1)userId + 현재 시각 기반으로 ACTIVE 세션 생성
    * 2)점수,거리,코인,플래그는 초기값으로 세팅
    * 3)생성된 세션 ID 반환 -> 클라리언트가 이후 이벤트 전송 시 사용
    * */
    @Transactional
    public Long startSession(Long userId, StartSessionRequest req) {
        Session s = Session.builder()
                .userId(userId)
                .status(Session.Status.ACTIVE)
                .startedAt(LocalDateTime.now())
                .deviceInfo(req.getDeviceInfoJson()) //단말 정보 로그용
                .score(0).distance(0).coins(0)
                .maxSpeed(null)
                .flags(null)
                .build();
        Session saved = sessionRepository.save(s);
        return saved.getId();
    }

    /*
    * 세션 종료+이벤트 기반 최종 집계
    *
    * 1)최초 종료 요청
    *   - endedAt/Status.ENDED 세팅
    *   - 이벤트 전체를 다시 읽어 aggregateSession(...)으로 집계
    *   - 클라이언트가 보낸 체크섬과 서버 계산 체크섬을 비교해서 checksumValid 플래그 설정
    *   - Session 엔티티에 최종 점수,거리,코인,최대속도,플래그 반영
    *   - 랭킹용 SessionScore 레코드 기록
    * 2)이미 종료된 세션에 대한 재요청
    *   - DB에 저장된 집계 값은 그대로 사용
    *   - activeDurationMs 만 이벤트 기반으로 다시 계산해서 응답
    * */
    @Transactional
    public SessionSummaryResponse endSession(Long userId, Long sessionId, EndSessionRequest req){
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("session not found"));

        //본인 세션인지 검증
        if(!Objects.equals(session.getUserId(), userId))
            throw new IllegalStateException("not your session");

        //이미 종료된 세션이면 -> 멱등 처리(저장값 기준으로 응답만 생성)
        if(session.getEndedAt() != null){
            List<SessionEvent> events = sessionEventRepository.findBySessionIdOrderBySeqAsc(sessionId);
            long activeMs = computeActiveDurationMs(events);

            return SessionSummaryResponse.builder()
                    .sessionId(session.getId())
                    .score(session.getScore())
                    .coins(session.getCoins())
                    .distance(session.getDistance())
                    .maxSpeed(session.getMaxSpeed() == null ? null : session.getMaxSpeed().doubleValue())
                    .activeDurationMs(activeMs)
                    .checksumValid(session.isChecksumValid())
                    .flags(session.getFlags())
                    .build();
        }

        //1)최초 종료 경로
        session.setEndedAt(LocalDateTime.now());
        session.setStatus(Session.Status.ENDED);
        sessionRepository.save(session);

        //2)이벤트 전부 로드
        List<SessionEvent> events = sessionEventRepository.findBySessionIdOrderBySeqAsc(sessionId);

        //3)서버 기준 집계
        AggregationResult agg = aggregateSession(events);

        //4)체크섬 비교(클라이언트가 기록을 보낸 경우에만 검증)
        String clientChecksum = (req != null ? req.getClientChecksum() : null);
        boolean checksumValid = true;
        if(clientChecksum != null){
            String serverChecksum = computeChecksum(events);
            checksumValid = serverChecksum.equals(clientChecksum);
        }

        //5)세션 엔티티 갱신
        session.setScore(agg.score());
        session.setCoins(agg.coins());
        session.setDistance(agg.distance());
        session.setMaxSpeed(agg.maxSpeed() == null ? null : BigDecimal.valueOf(agg.maxSpeed()));
        session.setFlags(String.join(",", agg.flags()));
        session.setChecksumValid(checksumValid);
        sessionRepository.save(session);

        //6)랭킹용 점수 기록(최초 종료시에만)
        writeScores(session.getUserId(), session.getId(), agg.score());

        //7)클라이언트에 내려줄 요약 응답
        return SessionSummaryResponse.builder()
                .sessionId(session.getId())
                .score(agg.score()).coins(agg.coins()).distance(agg.distance())
                .maxSpeed(agg.maxSpeed())
                .activeDurationMs(agg.activeDurationMs())
                .checksumValid(checksumValid)
                .flags(session.getFlags())
                .build();
    }

    //세션 집계 결과를 담는 내부용 DTO
    public static record AggregationResult(
            int score,
            int coins,
            int distance,
            Double maxSpeed,
            long activeDurationMs,
            List<String> flags) {
    }

    /*
    * 이벤트 전체를 돌면서 세션을 다시 집계하는 메서드
    *
    * - powerup_pick
    *   - SCORE_X2 : 특정 시점까지 점수 2배
    *   - SHIELD : 다음 hit_obstacle 1회 무효화
    *   - MAGNET : 서버 점수에는 영향 없음
    *   - SPEED_BOOST : MovementScorer의 거리 가중치 증가
    * - coin_pick
    *   - value 만큼 코인 증가
    *   - SCORE_X2 활성 구간은 점수 2배로 반영
    * - hib_obstacle
    *   - shield가 남아 있으면 소비하고 점수 차감 없음
    *   - 아니면 10점 감점
    * - sprint_end
    *   - payload.avgSpeed를 이용해 maxSpeed 갱신
    * - checkpoint
    *   - 기본 10점, SCORE_X2면 20점
    * - 최종 보정
    *   - totalScore는 0 미만으로 떨어지지 않게 최소 0으로 보정
    *   - hit 횟수가 많으면 플래그 부여
    * */
    private AggregationResult aggregateSession(List<SessionEvent> events){
        if(events.isEmpty()) {
            return new AggregationResult(0,0,0,null,0,List.of());
        }

        int coins = 0;
        int hits = 0;
        int checkpoints = 0;
        Double maxSpeed = null;

        //1)pause,resumt 를 반영한 실제 플레이 시간 계산
        long activeMs = computeActiveDurationMs(events);

        //2)이동,점수 계산을 위한 상태
        boolean reverse = false, sprint = false, slide = false, paused = false;

        //3)거리,점수 계산기 준비
        int lastTms = events.get(0).getTMs();
        MovementScorer scorer = new MovementScorer(lastTms);
        scorer.seedState(reverse, sprint, slide, paused);

        int totalDistancePx = 0;
        int totalScore = 0;

        //4)배점 관련 상태
        int scoreX2UntilMs = -1;
        Deque<Integer> shieldUntil = new ArrayDeque<>();

        //5)이벤트 순회
        for(SessionEvent e : events){
            int t = e.getTMs();

            //(1)이동거리/점수반영
            MovementScorer.Result move = scorer.flushUntil(t, -1); //이동 점수는 x2 제외(코인,체크포인트에만 적용)
            totalDistancePx += move.distPx;
            totalScore += move.scoreDelta; //이동 점수 반영(기본: 10px = 1점)

            //(2)이벤트 타입별 처리
            switch (e.getType()) {
                case powerup_pick -> {
                    String name = Payloads.getString(e.getPayload(), "name", "");
                    int dur = Math.max(0, Payloads.getInt(e.getPayload(), "durationMs", 5000));

                    if ("SCORE_X2".equals(name)) {
                        scoreX2UntilMs = Math.max(scoreX2UntilMs, t + dur);
                    }
                    else if ("SHIELD".equals(name)) {
                        shieldUntil.addLast(t + dur);
                    }
                    else if("MAGNET".equals(name)){
                        //서버 점수에는 영향 없음
                        //클라 편의 기능으로만 사용
                    }
                    else if("SPEED_BOOST".equals(name)){
                        //SPEED_BOOST 활성화 -> MovementScorer에서 거리 가중치 증가
                        scorer.setBoost(true);
                    }
                }
                case coin_pick -> {
                    int v = Payloads.getInt(e.getPayload(), "value", 1);
                    coins += v;
                    boolean x2 = (t <= scoreX2UntilMs);
                    totalScore += v * (x2 ? 2 : 1);
                }

                case hit_obstacle -> {
                    //만료된 쉴드 제거
                    while (!shieldUntil.isEmpty() && shieldUntil.peekFirst() < t)
                        shieldUntil.pollFirst();

                    //쉴드가 남아있으면 1회 소비하고 감점 없음
                    if (!shieldUntil.isEmpty()) {
                        shieldUntil.pollFirst();
                    } else {
                        hits++;
                        totalScore -= 10;
                    }
                }

                case sprint_start -> scorer.setSprint(true);
                case sprint_end -> {
                    scorer.setSprint(false);
                    Double avg = Payloads.getDoubleOrNull(e.getPayload(), "avgSpeed");
                    if (avg != null) {
                        maxSpeed = (maxSpeed == null ? avg : Math.max(maxSpeed, avg));
                    }
                }

                case reverse_start -> scorer.setReverse(true);
                case reverse_end -> scorer.setReverse(false);

                case slide_start -> scorer.setSlide(true);
                case slide_end -> scorer.setSlide(false);

                case pause -> scorer.setPaused(true);
                case resume -> scorer.setPaused(false);

                case checkpoint -> {
                    checkpoints++;
                    boolean x2 = (t <= scoreX2UntilMs);
                    totalScore += 10 * (x2 ? 2 : 1);
                }

                default -> {}
            }
        }

        //6)마지막 이벤트 이후 남은 이동 거리,점수 반영
        int lastT = events.get(events.size()-1).getTMs();
        MovementScorer.Result tail = scorer.flushUntil(lastT, -1);
        totalDistancePx += tail.distPx;
        totalScore += tail.scoreDelta;

        //7)최종 보정
        totalScore = Math.max(0, totalScore);
        int distance = (int) Math.round(totalDistancePx / 10.0); //10px=1m 기준 환산

        //8)플래그 설정
        //ex)충돌이 너무 많으면 플래그 추가
        List<String> flags = new ArrayList<>();
        if (hits > 10) flags.add("TOO_MANY_HITS");

        return new AggregationResult(totalScore, coins, distance, maxSpeed, activeMs, flags);
    }

    /*
    * pause/resume를 고려한 실제 플레이 시간 계산
    *
    * - t_ms는 세션 시작 기준 경과 ms라고 가정
    * - pause 이후 ~ resume 이전 구간은 active 시간에서 제외
    * - 이벤트는 seq 기준 정렬되어 있다고 가정
    * */
    private long computeActiveDurationMs(List<SessionEvent> events) {
        long last = 0;
        boolean paused = false;
        long active = 0;

        for(int i = 0; i < events.size(); i++){
            SessionEvent e = events.get(i);
            long t = e.getTMs();

            if(i == 0) {last = t; continue;}

            long delta = t - last;
            if(!paused && delta > 0) active += delta;

            if(e.getType() == EventType.pause) paused = true;
            if(e.getType() == EventType.resume) paused = false;

            last = t;
        }
        return Math.max(0, active);
    }

    /*
    * 이벤트 로그 기반 체크섬 계산
    * - seq|tMs|type|payload를 문자열로 이어붙여 hashCode() 후 16진수 문자열로 반환
    * - 클라이언트에서 보낸 clientChecksum과 비교해 이벤트 위,변조 여부를 간단하게 검증하는 용도
    * */
    private String computeChecksum(List<SessionEvent> events){
        StringBuilder sb = new StringBuilder();
        for(SessionEvent e : events){
            sb.append(e.getSeq()).append("|").append(e.getTMs()).append("|")
                    .append(e.getType().name()).append("|").append(Objects.toString(e.getPayload(), ""))
                    .append("\n");
        }
        return Integer.toHexString(sb.toString().hashCode());
    }

    //랭킹,통계용 점수 기록
    private void writeScores(Long userId, Long sessionId, int score){
        ZoneId KST = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(KST);
        WeekFields wf = WeekFields.ISO;

        int year = today.getYear();
        int week = today.get(wf.weekOfWeekBasedYear());
        int month = today.getMonthValue();

        //일 단위
        sessionScoreRepository.save(SessionScore.builder()
                .sessionId(sessionId).userId(userId).score(score)
                .periodDay(today).build());
        //주 단위
        sessionScoreRepository.save(SessionScore.builder()
                .sessionId(sessionId).userId(userId).score(score)
                .periodWeek(week).build());
        //월 단위
        sessionScoreRepository.save(SessionScore.builder()
                .sessionId(sessionId).userId(userId).score(score)
                .periodYear(year).periodMonth(month).build());
    }

    /*
    * 관리자 강제 종료 + 집계 재계산
    *
    * 1) 이미 종료된 세션이면 -> 기존 값 기반으로 요약만 반환
    * 2) ACTIVE인 경우
    *   - endedAt/Status.ENDED 세팅
    *   - 이벤트 기반으로 aggregateSession(...) 재계산
    *   - 세션 엔티티에 반영 + 랭킹용 점수 기록
    * */
    @Transactional
    public SessionSummaryResponse adminForceEnd(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("session not found"));

        //이미 종료된 세션 -> 멱등 처리
        if (session.getEndedAt() != null) {
            var events = sessionEventRepository.findBySessionIdOrderBySeqAsc(sessionId);
            long activeMs = computeActiveDurationMs(events);
            return SessionSummaryResponse.builder()
                    .sessionId(session.getId())
                    .score(session.getScore())
                    .coins(session.getCoins())
                    .distance(session.getDistance())
                    .maxSpeed(session.getMaxSpeed() == null ? null : session.getMaxSpeed().doubleValue())
                    .activeDurationMs(activeMs)
                    .checksumValid(session.isChecksumValid())
                    .flags(session.getFlags())
                    .build();
        }

        //강제 종료 처리
        session.setEndedAt(LocalDateTime.now());
        session.setStatus(Session.Status.ENDED);
        sessionRepository.save(session);

        //재계산 및 반영
        var events = sessionEventRepository.findBySessionIdOrderBySeqAsc(sessionId);
        AggregationResult agg = aggregateSession(events);
        session.setScore(agg.score());
        session.setCoins(agg.coins());
        session.setDistance(agg.distance());
        session.setMaxSpeed(agg.maxSpeed() == null ? null : java.math.BigDecimal.valueOf(agg.maxSpeed()));
        session.setFlags(String.join(",", agg.flags()));
        session.setChecksumValid(true);
        sessionRepository.save(session);

        writeScores(session.getUserId(), session.getId(), agg.score());

        return SessionSummaryResponse.builder()
                .sessionId(session.getId())
                .score(agg.score()).coins(agg.coins()).distance(agg.distance())
                .maxSpeed(agg.maxSpeed())
                .activeDurationMs(agg.activeDurationMs())
                .checksumValid(true)
                .flags(session.getFlags())
                .build();
    }

    /*
    * 운영/관리 서비스에서 재사용하기 위해 공개한 헬퍼
    * - SessionRecalculationService 등에서 이 세션을 이벤트 기준으로 다시 집계해줘 라고 요청할 때 사용
    * */
    public AggregationResult aggregateForAdmin(List<SessionEvent> events) {
        return aggregateSession(events);
    }

    /*
    * 운영,관리 서비스에서 재사용하기 위해 공개한 헬퍼
    * - activeDurationMs만 필요할 때 사용
    * */
    public long computeActiveMsForAdmin(List<SessionEvent> events) {
        return computeActiveDurationMs(events);
    }

}
