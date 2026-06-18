package com.example.rungame.session.service;

import com.example.rungame.event.domain.SessionEvent;
import com.example.rungame.event.repository.SessionEventRepository;
import com.example.rungame.leaderboard.service.RedisLeaderboardService;
import com.example.rungame.score.domain.SessionScore;
import com.example.rungame.score.repository.SessionScoreRepository;
import com.example.rungame.session.domain.Session;
import com.example.rungame.session.dto.EndSessionRequest;
import com.example.rungame.session.dto.SessionSummaryResponse;
import com.example.rungame.session.dto.StartSessionRequest;
import com.example.rungame.session.repository.SessionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final SessionEventRepository sessionEventRepository;
    private final SessionScoreRepository sessionScoreRepository;
    private final RedisLeaderboardService redisLeaderboardService;

    //세션 시작
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

    //세션 종료+이벤트 기반 최종 집계
    @Transactional
    public SessionSummaryResponse endSession(Long userId, Long sessionId, EndSessionRequest req){
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("session not found"));

        //본인 세션인지 검증
        if(!Objects.equals(session.getUserId(), userId))
            throw new IllegalStateException("not your session");

        //이미 종료된 세션이면 -> 멱등 처리(저장값 기준으로 응답만 생성)
        if (session.getEndedAt() != null) {
            long activeMs = 0L;

            if (session.getStartedAt() != null && session.getEndedAt() != null) {
                activeMs = java.time.Duration.between(
                        session.getStartedAt(),
                        session.getEndedAt()
                ).toMillis();
            }

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

        //최초 종료 경로
        LocalDateTime endedAt = LocalDateTime.now();

        int updated = sessionRepository.endActiveSession(sessionId, userId, endedAt);

        //동시에 들어온 다른 요청이 먼저 종료 처리한 경우
        if (updated == 0) {
            Session latest = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("session not found"));

            long activeMs = 0L;

            if (latest.getStartedAt() != null && latest.getEndedAt() != null) {
                activeMs = java.time.Duration.between(
                        latest.getStartedAt(),
                        latest.getEndedAt()
                ).toMillis();
            }

            return SessionSummaryResponse.builder()
                    .sessionId(latest.getId())
                    .score(latest.getScore())
                    .coins(latest.getCoins())
                    .distance(latest.getDistance())
                    .maxSpeed(latest.getMaxSpeed() == null ? null : latest.getMaxSpeed().doubleValue())
                    .activeDurationMs(activeMs)
                    .checksumValid(latest.isChecksumValid())
                    .flags(latest.getFlags())
                    .build();
        }

        //조건부 update로 종료 처리됨
        //현재 session 객체도 응답/후속 처리에 맞게 값 동기화
        session.setEndedAt(endedAt);
        session.setStatus(Session.Status.ENDED);

        String clientChecksum = (req != null ? req.getClientChecksum() : null);
        boolean checksumValid = true;


        //clientChecksum이 있는 경우에는 이벤트 위변조 검증을 위해 기존처럼 이벤트 조회
        //checksum이 없는 일반 종료 요청은 이벤트 전체 재조회 없이 sessions 누적값을 그대로 사용
        if (clientChecksum != null) {
            List<SessionEvent> events = sessionEventRepository.findBySessionIdOrderBySeqAsc(sessionId);
            String serverChecksum = computeChecksum(events);
            checksumValid = serverChecksum.equals(clientChecksum);
        }

        session.setChecksumValid(checksumValid);

        //랭킹용 점수 기록(최초 종료시에만)
        writeScores(session.getUserId(), session.getId(), session.getScore());

        try {
            redisLeaderboardService.updateLeaderboards(
                    session.getUserId(),
                    session.getScore(),
                    session.getEndedAt()
            );
        } catch (Exception e) {
            log.warn("Redis 리더보드 반영 실패. DB 기록은 유지됩니다. sessionId={}", session.getId(), e);
        }

        long activeDurationMs = 0;
        if (session.getStartedAt() != null && session.getEndedAt() != null){
            activeDurationMs = java.time.Duration.between(
                    session.getStartedAt(),
                    session.getEndedAt()
            ).toMillis();
        }

        return SessionSummaryResponse.builder()
                .sessionId(session.getId())
                .score(session.getScore())
                .coins(session.getCoins())
                .distance(session.getDistance())
                .maxSpeed(session.getMaxSpeed() == null ? null : session.getMaxSpeed().doubleValue())
                .activeDurationMs(activeDurationMs)
                .checksumValid(checksumValid)
                .flags(session.getFlags())
                .build();
    }

    //이벤트 로그 기반 체크섬 계산
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

}
