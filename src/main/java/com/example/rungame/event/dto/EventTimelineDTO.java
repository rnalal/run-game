package com.example.rungame.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

//한 세션에 대한 전체 이벤트 타임라인을 표현하는 DTO
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventTimelineDTO {

    private Long sessionId;
    private Long userId;
    //세션의 실제 플레이 시간
    private long durationMs;
    //해당 세션에서 기록된 전체 이벤트 개수
    private Integer totalEvents;
    //시간 순으로 정렬된 이벤트 로그 목록
    private List<EventLogDTO> events;
}
