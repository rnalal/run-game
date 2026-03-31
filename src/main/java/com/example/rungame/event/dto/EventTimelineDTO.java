package com.example.rungame.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/*
* 한 세션에 대한 전체 이벤트 타임라인을 표현하는 DTO
* - 특정 세션 기준으로 누가 얼마동안 어떤 이벤트들을 발생시켰는지를
*   한번에 전달하는 조회 전용 뷰 모델
* */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventTimelineDTO {

    private Long sessionId;
    private Long userId;
    /*
    * 세션의 실제 플레이 시간
    * - pause/resume 구간을 제외한 순수 active 시간
    * - 서버 계산값으로 클라이언트에서 임의로 보내지 않는 필드
    * */
    private long durationMs;
    /*
    * 해당 세션에서 기록된 전체 이벤트 개수
    * - events.size()와 같을 가능성이 높지만
    *   필터링/페이지네이션이 들어가면 의미가 달라질 수 있어
    *   원본 상의 총 이벤트 수로도 활용 가능
    * */
    private Integer totalEvents;
    /*
    * 시간 순으로 정렬된 이벤트 로그 목록
    * */
    private List<EventLogDTO> events;
}
