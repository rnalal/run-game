package com.example.rungame.admin.service;

import com.example.rungame.common.util.CsvExporter;
import com.example.rungame.event.domain.EventType;
import com.example.rungame.event.domain.SessionEvent;
import com.example.rungame.event.dto.EventLogDTO;
import com.example.rungame.event.dto.EventTimelineDTO;
import com.example.rungame.event.repository.SessionEventRepository;
import com.example.rungame.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import com.example.rungame.session.domain.Session;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminEventService {

    private final SessionEventRepository eventRepo;
    private final SessionRepository sessionRepo;

    //이벤트 로그 목록 조회
    public Page<EventLogDTO> list(
            String type,
            Long userId,
            Long sessionId,
            LocalDateTime fromAt,
            LocalDateTime toAt,
            int page,
            int size
    ) {
        //이벤트 타입 문자열 -> Enum 변환
        EventType t = null;
        if (type != null && !type.isBlank()) {
            t = EventType.valueOf(type); // 잘못된 값이면 400 처리 필요 => 컨트롤러에서 try/catch
        }
        //최신 이벤트 우선 정렬
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by("id").descending()));

        Specification<SessionEvent> spec = buildSearchSpec(t, userId, sessionId, fromAt, toAt);

        Page<SessionEvent> result = eventRepo.findAll(spec, pageable);

        return result.map(e -> EventLogDTO.from(e, null));
    }

    //특정 세션의 이벤트 타임라인 조회
    public EventTimelineDTO timeline(Long sessionId) {
        //세션 조회
        var session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("session not found"));
        //세션에 속한 이벤트 전체 조회
        var events = eventRepo.findBySessionIdOrderBySeqAsc(sessionId);

        //실제 플레이 시간 계산
        long activeMs = computeActiveDurationMs(events);

        //이벤트 DTO 변환
        List<EventLogDTO> list = events.stream()
                .map(e -> EventLogDTO.from(e, session.getUserId()))
                .toList();

        //타임라인 DTO 구성
        return EventTimelineDTO.builder()
                .sessionId(sessionId)
                .userId(session.getUserId())
                .durationMs(activeMs)
                .totalEvents(list.size())
                .events(list)
                .build();
    }

    //이벤트 로그 CSV 내보내기
    public byte[] exportCsv(
            String type,
            Long userId,
            Long sessionId,
            LocalDateTime fromAt,
            LocalDateTime toAt
    ) {
        //이벤트 타입 변환
        EventType t = null;
        if (type != null && !type.isBlank()) {
            t = EventType.valueOf(type);
        }
        //조건에 맞는 이벤트 전체 조회
        Specification<SessionEvent> spec = buildSearchSpec(t, userId, sessionId, fromAt, toAt);

        List<SessionEvent> all = eventRepo.findAll(
                spec,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        //CSV 헤더 정의
        String header = "id,sessionId,seq,tMs,type,createdAt,payload";
        //CSV 변환
        return CsvExporter.toCsv(header, all, e -> String.join(",",
                String.valueOf(e.getId()),
                String.valueOf(e.getSessionId()),
                String.valueOf(e.getSeq()),
                String.valueOf(e.getTMs()),
                e.getType().name(),
                Objects.toString(e.getCreatedAt(), ""),
                CsvExporter.csvEscape(e.getPayload())
        ));
    }

    //이벤트 검색 조건 Specification 생성
    private Specification<SessionEvent> buildSearchSpec(
            EventType type,
            Long userId,
            Long sessionId,
            LocalDateTime fromAt,
            LocalDateTime toAt
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }

            if (sessionId != null) {
                predicates.add(cb.equal(root.get("sessionId"), sessionId));
            }

            if (fromAt != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromAt));
            }

            if (toAt != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), toAt));
            }

            if (userId != null) {
                var subquery = query.subquery(Long.class);
                var sessionRoot = subquery.from(Session.class);

                subquery.select(sessionRoot.get("id"))
                        .where(
                                cb.equal(sessionRoot.get("id"), root.get("sessionId")),
                                cb.equal(sessionRoot.get("userId"), userId)
                        );

                predicates.add(cb.exists(subquery));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    //pause,resume 구간 제외한 실제 플레이 시간 계산
    private long computeActiveDurationMs(List<SessionEvent> events) {
        long last = 0;
        boolean paused = false;
        long active = 0;

        for (int i=0; i<events.size(); i++){
            SessionEvent e = events.get(i);
            long t = e.getTMs();

            if (i==0){
                last = t;
                continue;
            }

            long delta = t - last;
            if (!paused && delta > 0){
                active += delta;
            }

            if (e.getType() == EventType.pause){
                paused = true;
            }

            if (e.getType() == EventType.resume) {
                paused = false;
            }

            last = t;
        }

        return Math.max(0, active);
    }
}
