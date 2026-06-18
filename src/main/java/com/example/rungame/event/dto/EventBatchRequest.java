package com.example.rungame.event.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

//세션 이벤트를 한 번에 여러 개 전송하기 위한 배치 요청 DTO
@Getter
@Setter
public class EventBatchRequest {

    //전송된 이벤트들의 리스트
    private List<EventDTO> events;
}
