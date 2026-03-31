package com.example.rungame.admin.service;

import com.example.rungame.common.util.CsvExporter;
import com.example.rungame.event.domain.EventType;
import com.example.rungame.event.domain.SessionEvent;
import com.example.rungame.event.dto.EventLogDTO;
import com.example.rungame.event.dto.EventTimelineDTO;
import com.example.rungame.event.repository.SessionEventRepository;
import com.example.rungame.session.repository.SessionRepository;
import com.example.rungame.session.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/*
* 관리자 이벤트 관리 서비스
*
* - 이벤트 로그 조회 (필터 + 페이징)
* - 세션 단위 이벤트 타임라인 생성
* - 이벤트 데이터 CSV 내보내기
*
* 운영/분석 목적의 조회에 초점을 둔 서비스로,
* 게임 로직 변경 없이 이벤트 데이터를 가공,제공함
* */
@Service
@RequiredArgsConstructor
public class AdminEventService {

    //세션 이벤트 조회 Repository
    private final SessionEventRepository eventRepo;
    //세션 정보 조회 Repository
    private final SessionRepository sessionRepo;
    /*
    * 세션 관련 비즈니스 로직 서비스
    *
    * - 관리자용 플레이 시간 계산 등에 사용
    * */
    private final SessionService sessionService;

    //============이벤트 로그 조회 (필터 + 페이징)===============
    /*
    * 이벤트 로그 목록 조회
    *
    * @param type : 이벤트 타입 (문자열, optional)
    * @param userId : 사용자 ID (optional)
    * @param sessionId : 세션 ID (optional)
    * @param formAt : 조회 시작 시각
    * @param toAt : 조회 종료 시각
    * @param page : 페이지 번호
    * @param size : 페이지 크기
    * */
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
        //(잘못된 값에 대한 예외 처리는 컨트롤러에서 담당)
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

        //필터 조건 기반 이벤트 조회
        Page<SessionEvent> result = eventRepo.searchPage(t, userId, sessionId, fromAt, toAt, pageable);

        /*
        * userId는 조인 비용을 줄이기 위해 여기서는 null로 두고,
        * 프론트에서는 sessionId 기반 상세 링크 제공을 기본으로 함
        * */
        return result.map(e -> EventLogDTO.from(e, null));
    }

    //=================이벤트 타임라인 조회 (세션 단위)==================
    /*
    * 특정 세션의 이벤트 타임라인 조회
    *
    * - 이벤트 발생 순서 기반 분석
    * - 실제 플레이 시간 계산 포함
    * */
    public EventTimelineDTO timeline(Long sessionId) {
        //세션 조회
        var session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("session not found"));
        //세션에 속한 이벤트 전체 조회 (seq 기준 오름차순)
        var events = eventRepo.findBySessionIdOrderBySeqAsc(sessionId);

        //실제 플레이 시간 계산 (관리자용)
        long activeMs = 0L;
        try {
            activeMs = sessionService.computeActiveMsForAdmin(events);
        } catch (Exception ignore){
            //계산 실패 시 0으로 처리
        }

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

    //=====================이벤트 내보내기==========================
    /*
    * 이벤트 로그 CSV 내보내기
    *
    * - JSON 응답은 컨트롤러에서 Jackson이 처리
    * - CSV는 여기서 byte[] 생성
    * */
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
        List<SessionEvent> all = eventRepo.searchAll(t, userId, sessionId, fromAt, toAt);

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
}
