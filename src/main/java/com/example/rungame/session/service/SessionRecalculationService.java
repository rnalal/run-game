package com.example.rungame.session.service;

import com.example.rungame.event.domain.SessionEvent;
import com.example.rungame.event.repository.SessionEventRepository;
import com.example.rungame.session.domain.Session;
import com.example.rungame.session.dto.SessionSummaryResponse;
import com.example.rungame.session.repository.SessionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/*
* 세션 재집계 전용 서비스
* - 이미 저장된 세션과 이벤트 로그를 기반으로 세션의 점수,거리,코인,최대속도,플래그를 다시
*   계산하거나 초기화하는 관리자용 서비스
*
* - 1)recalc(sessionId)
*   - 해당 세션의 이벤트들을 전부 다시 읽어서 SessionService.aggregateForAdmin(...)으로 정답 집계를 구한 뒤
*     세션의 score/coins/distance/maxSpeed/flags/checksumValid를 갱신
*   - 최종 결과를 SessionSummaryResponse로 반환
* - 2)resetAggregates(sessionId)
*   - 이벤트 로그는 그대로 두고 세션의 score/coins/distance/maxSpeed/flags를 0/NULL로 초기화
*   - activeDurationMs는 이벤트를 이용해서 다시 계산
* */
@Service
@RequiredArgsConstructor
public class SessionRecalculationService {

    private final SessionRepository sessionRepository;
    private final SessionEventRepository sessionEventRepository;
    private final SessionService sessionService;

    /*
    * (1)이벤트 로그를 기준으로 세션 집계를 다시 계산하고 세션을 갱신
    *
    * 1)세션 조회
    * 2)해당 세션의 이벤트 전체 조회
    * 3)SessionService.aggregateForAdmin(...)을 호출해 점수,코인,거리,최대속도,활성시간,
    *   플래그 목록을 한 번에 집계
    * 4)Session 엔티티의 집계 필드(score,coins,distance,maxSpeed..)를 갱신 후 저장
    * 5)화면에서 바로 쓸 수 있도록 SessionSummaryResponse로 변환해서 반환
    * */
    @Transactional
    public SessionSummaryResponse recalc(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("session not found"));

        //1) 이벤트 전부 로드
        List<SessionEvent> events = sessionEventRepository.findBySessionIdOrderBySeqAsc(sessionId);

        //2)관리자용 집계 로직으로 한 번에 재계산
        var agg = sessionService.aggregateForAdmin(events);

        //3)세션 엔티티 갱신
        session.setScore(agg.score());
        session.setCoins(agg.coins());
        session.setDistance(agg.distance());
        session.setMaxSpeed(agg.maxSpeed() == null ? null : BigDecimal.valueOf(agg.maxSpeed()));
        session.setFlags(String.join(",", agg.flags()));
        session.setChecksumValid(true);
        sessionRepository.save(session);

        //4)클라이언트,어드민 응답용 DTO로 변환
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
    * (2)세션의 집계 필드만 초기화
    *
    * 1)세션 조회
    * 2)score/coins/distance/maxSpeed/flags를 전부 초기값으로 되돌림
    * 3)checksumValid는 true로 세팅
    * 4)이벤트 목록을 다시 읽어서 activeDurationMs만 재계산
    * 5)0점,0코인,0거리 기준의 SessionSummaryResponse 반환
    * */
    @Transactional
    public SessionSummaryResponse resetAggregates(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("session not found"));

        //1)집계 필드 초기화
        session.setScore(0);
        session.setCoins(0);
        session.setDistance(0);
        session.setMaxSpeed(null);
        session.setFlags(null);
        session.setChecksumValid(true);
        sessionRepository.save(session);

        //2)활성 시간은 이벤트로부터 다시 계산
        long activeMs = 0L;
        var events = sessionEventRepository.findBySessionIdOrderBySeqAsc(sessionId);
        if(!events.isEmpty()) {
            activeMs = sessionService.computeActiveMsForAdmin(events);
        }

        //3)초기화된 값 기준으로 요약 응답 반환
        return SessionSummaryResponse.builder()
                .sessionId(session.getId())
                .score(0).coins(0).distance(0)
                .maxSpeed(null)
                .activeDurationMs(activeMs)
                .checksumValid(true)
                .flags(null)
                .build();
    }
}
