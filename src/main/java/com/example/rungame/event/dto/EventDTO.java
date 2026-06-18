package com.example.rungame.event.dto;

import com.example.rungame.event.domain.EventType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

//단일 게임 이벤트를 표현하는 전송 DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDTO {

    private int seq;

    //Jackson 매핑을 위해 JsonProperty 사용
    @JsonProperty("tMs")
    private int tMs;

    //이벤트 타입
    private EventType type;

    //이벤트 상세 payload (JSON 문자열 그대로)
    @JsonProperty("payloadJson")
    private String payloadJson; // raw JSON string (client -> server)
}
