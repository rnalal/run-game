package com.example.rungame.event.dto;

import com.example.rungame.event.domain.EventType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/*
* 단일 게임 이벤트를 표현하는 전송 DTO
* - 클라이언트와 서버 사이에서 오가는 이벤트 한 건의 표준 형태
* - 세션 내에서 몇 번째 이벤트인지, 언제 발생했는지,
*   어떤 타입인지, 어떤 payload 인지 포함
* */
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

    /*
     * 이벤트 타입
     * - EventType Enum 으로 관리
     * - 프론트에서 문자열로 전송되는 값과 1:1 매핑
     * */
    private EventType type;

    /*
    * 이벤트 상세 payload (JSON 문자열 그대로)
    * - 클라이언트가 만든 JSON을 서버에서 한 번 더 파싱하지 않고
    *   row 문자열 상태로 우선 저장하거나 전달할 수 있도록 설계
    *
    * 저장 단계에서
    * - SessionEvent.payload로 그대로 넘겨 DB의 JSON 컬럼에 저장 가능
    * */
    @JsonProperty("payloadJson")
    private String payloadJson; // raw JSON string (client -> server)
}
