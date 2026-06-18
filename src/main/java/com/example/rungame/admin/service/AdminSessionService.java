package com.example.rungame.admin.service;

import com.example.rungame.common.support.Payloads;
import com.example.rungame.event.domain.EventType;
import com.example.rungame.event.domain.SessionEvent;
import com.example.rungame.event.repository.SessionEventRepository;
import com.example.rungame.event.service.MovementScorer;
import com.example.rungame.leaderboard.service.RedisLeaderboardService;
import com.example.rungame.score.domain.SessionScore;
import com.example.rungame.score.repository.SessionScoreRepository;
import com.example.rungame.session.domain.Session;
import com.example.rungame.session.dto.SessionSummaryResponse;
import com.example.rungame.session.repository.SessionRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSessionService {

    private final SessionRepository sessionRepository;
    private final SessionEventRepository sessionEventRepository;
    private final SessionScoreRepository sessionScoreRepository;
    private final RedisLeaderboardService redisLeaderboardService;

    //관리자 세션 목록 조회
    public Page<Session> list(
            Long userId,
            String status,
            LocalDateTime fromAt,
            LocalDateTime toAt,
            int page,
            int size
    ) {
        Session.Status sessionStatus = null;

        if (status != null && !status.isBlank()) {
            sessionStatus = Session.Status.valueOf(status.toUpperCase());
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "startedAt")
                        .and(Sort.by("id").descending())
        );

        Specification<Session> spec = buildSearchSpec(userId, sessionStatus, fromAt, toAt);

        return sessionRepository.findAll(spec, pageable);
    }

    //세션 상세 조회
    public Map<String, Object> detail(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("session not found"));

        List<SessionEvent> events =
                sessionEventRepository.findBySessionIdOrderBySeqAsc(sessionId);

        return Map.of(
                "session", session,
                "events", events
        );
    }

    //세션 모니터링 자표 조회
    public Map<String, Object> monitor(int minutes) {
        long activeCount = sessionRepository.countByStatus(Session.Status.ACTIVE);
        long avgActiveSec = sessionRepository.avgActiveSecondsForActiveSessions();

        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        long recentHits = sessionEventRepository.countByTypeAndCreatedAtAfter(
                EventType.hit_obstacle,
                since
        );

        return Map.of(
                "activeSessions", activeCount,
                "avgActiveSeconds", avgActiveSec,
                "recentHitCount", recentHits,
                "windowMinutes", minutes
        );
    }

    //일자별 세션 통계 조회
    public List<Map<String, Object>> dailyStats(LocalDate from, LocalDate to) {
        return sessionRepository.aggregateDailyStats(from, to);
    }

    //관리자 강제 종료
    @Transactional
    public SessionSummaryResponse forceEnd(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("session not found"));

        if (session.getEndedAt() != null) {
            List<SessionEvent> events =
                    sessionEventRepository.findBySessionIdOrderBySeqAsc(sessionId);

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

        session.setEndedAt(LocalDateTime.now());
        session.setStatus(Session.Status.ENDED);
        sessionRepository.save(session);

        List<SessionEvent> events =
                sessionEventRepository.findBySessionIdOrderBySeqAsc(sessionId);

        AggregationResult agg = aggregateSession(events);

        session.setScore(agg.score());
        session.setCoins(agg.coins());
        session.setDistance(agg.distance());
        session.setMaxSpeed(agg.maxSpeed() == null ? null : BigDecimal.valueOf(agg.maxSpeed()));
        session.setFlags(String.join(",", agg.flags()));
        session.setChecksumValid(true);
        sessionRepository.save(session);

        writeScores(session.getUserId(), session.getId(), agg.score());

        try {
            redisLeaderboardService.updateLeaderboards(
                    session.getUserId(),
                    agg.score(),
                    session.getEndedAt()
            );
        } catch (Exception e) {
            log.warn("Redis 리더보드 반영 실패. DB 기록은 유지됩니다. sessionId={}", session.getId(), e);
        }

        return SessionSummaryResponse.builder()
                .sessionId(session.getId())
                .score(agg.score())
                .coins(agg.coins())
                .distance(agg.distance())
                .maxSpeed(agg.maxSpeed())
                .activeDurationMs(agg.activeDurationMs())
                .checksumValid(true)
                .flags(session.getFlags())
                .build();
    }

    //세션 집계 재계산
    @Transactional
    public SessionSummaryResponse recalc(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("session not found"));

        List<SessionEvent> events =
                sessionEventRepository.findBySessionIdOrderBySeqAsc(sessionId);

        AggregationResult agg = aggregateSession(events);

        session.setScore(agg.score());
        session.setCoins(agg.coins());
        session.setDistance(agg.distance());
        session.setMaxSpeed(agg.maxSpeed() == null ? null : BigDecimal.valueOf(agg.maxSpeed()));
        session.setFlags(String.join(",", agg.flags()));
        session.setChecksumValid(true);
        sessionRepository.save(session);

        return SessionSummaryResponse.builder()
                .sessionId(session.getId())
                .score(agg.score())
                .coins(agg.coins())
                .distance(agg.distance())
                .maxSpeed(agg.maxSpeed())
                .activeDurationMs(agg.activeDurationMs())
                .checksumValid(true)
                .flags(session.getFlags())
                .build();
    }

    //세션 집계 정보 초기화
    @Transactional
    public SessionSummaryResponse resetAggregates(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("session not found"));

        session.setScore(0);
        session.setCoins(0);
        session.setDistance(0);
        session.setMaxSpeed(null);
        session.setFlags(null);
        session.setChecksumValid(true);
        sessionRepository.save(session);

        List<SessionEvent> events =
                sessionEventRepository.findBySessionIdOrderBySeqAsc(sessionId);

        long activeMs = computeActiveDurationMs(events);

        return SessionSummaryResponse.builder()
                .sessionId(session.getId())
                .score(0)
                .coins(0)
                .distance(0)
                .maxSpeed(null)
                .activeDurationMs(activeMs)
                .checksumValid(true)
                .flags(null)
                .build();
    }

    //세션 검색 조건 Specification 생성
    private Specification<Session> buildSearchSpec(
            Long userId,
            Session.Status status,
            LocalDateTime fromAt,
            LocalDateTime toAt
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (userId != null) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (fromAt != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startedAt"), fromAt));
            }

            if (toAt != null) {
                predicates.add(cb.lessThan(root.get("startedAt"), toAt));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    //이벤트 집계 결과 DTO
    private record AggregationResult(
            int score,
            int coins,
            int distance,
            Double maxSpeed,
            long activeDurationMs,
            List<String> flags
    ) {}

    //이벤트 로그 기반 세션 집계
    private AggregationResult aggregateSession(List<SessionEvent> events) {
        if (events.isEmpty()) {
            return new AggregationResult(0, 0, 0, null, 0, List.of());
        }

        int coins = 0;
        int hits = 0;
        int checkpoints = 0;
        Double maxSpeed = null;

        long activeMs = computeActiveDurationMs(events);

        boolean reverse = false;
        boolean sprint = false;
        boolean slide = false;
        boolean paused = false;

        int lastTms = events.get(0).getTMs();
        MovementScorer scorer = new MovementScorer(lastTms);
        scorer.seedState(reverse, sprint, slide, paused);

        int totalDistancePx = 0;
        int totalScore = 0;

        int scoreX2UntilMs = -1;
        Deque<Integer> shieldUntil = new ArrayDeque<>();

        for (SessionEvent e : events) {
            int t = e.getTMs();

            MovementScorer.Result move = scorer.flushUntil(t, -1);
            totalDistancePx += move.distPx;
            totalScore += move.scoreDelta;

            switch (e.getType()) {
                case powerup_pick -> {
                    String name = Payloads.getString(e.getPayload(), "name", "");
                    int dur = Math.max(0, Payloads.getInt(e.getPayload(), "durationMs", 5000));

                    if ("SCORE_X2".equals(name)) {
                        scoreX2UntilMs = Math.max(scoreX2UntilMs, t + dur);
                    } else if ("SHIELD".equals(name)) {
                        shieldUntil.addLast(t + dur);
                    } else if ("SPEED_BOOST".equals(name)) {
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
                    while (!shieldUntil.isEmpty() && shieldUntil.peekFirst() < t) {
                        shieldUntil.pollFirst();
                    }

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

                default -> {
                }
            }
        }

        int lastT = events.get(events.size() - 1).getTMs();
        MovementScorer.Result tail = scorer.flushUntil(lastT, -1);
        totalDistancePx += tail.distPx;
        totalScore += tail.scoreDelta;

        totalScore = Math.max(0, totalScore);
        int distance = (int) Math.round(totalDistancePx / 10.0);

        List<String> flags = new ArrayList<>();
        if (hits > 10) {
            flags.add("TOO_MANY_HITS");
        }

        return new AggregationResult(totalScore, coins, distance, maxSpeed, activeMs, flags);
    }

    //실제 플레이 시간 계산
    private long computeActiveDurationMs(List<SessionEvent> events) {
        long last = 0;
        boolean paused = false;
        long active = 0;

        for (int i = 0; i < events.size(); i++) {
            SessionEvent e = events.get(i);
            long t = e.getTMs();

            if (i == 0) {
                last = t;
                continue;
            }

            long delta = t - last;
            if (!paused && delta > 0) {
                active += delta;
            }

            if (e.getType() == EventType.pause) {
                paused = true;
            }

            if (e.getType() == EventType.resume) {
                paused = false;
            }

            last = t;
        }

        return Math.max(0, active);
    }

    //랭킹용 점수 기록
    private void writeScores(Long userId, Long sessionId, int score) {
        ZoneId kst = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(kst);
        WeekFields wf = WeekFields.ISO;

        int year = today.getYear();
        int week = today.get(wf.weekOfWeekBasedYear());
        int month = today.getMonthValue();

        sessionScoreRepository.save(SessionScore.builder()
                .sessionId(sessionId)
                .userId(userId)
                .score(score)
                .periodDay(today)
                .build());

        sessionScoreRepository.save(SessionScore.builder()
                .sessionId(sessionId)
                .userId(userId)
                .score(score)
                .periodWeek(week)
                .build());

        sessionScoreRepository.save(SessionScore.builder()
                .sessionId(sessionId)
                .userId(userId)
                .score(score)
                .periodYear(year)
                .periodMonth(month)
                .build());
    }
}
