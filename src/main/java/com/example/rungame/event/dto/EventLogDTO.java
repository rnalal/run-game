package com.example.rungame.event.dto;

import com.example.rungame.event.domain.EventType;
import com.example.rungame.event.domain.SessionEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
* 세션 이벤트 로그 조회용 DTO
*
* - SessionEvent 엔티티 + 유저 정보를 합쳐
*   관리자/운영 화면, 로그 조회 API 응답에 사용하기 위한 전용 DTO
* - 특정 유저/세션에 대한 이벤트 로그 조회 API 응답
* */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventLogDTO {

    private Long id;
    private Long sessionId;
    private Integer seq;
    private Integer tMs;
    private EventType type;
    /*
    * 이벤트 payload (JSON 문자열)
    * - SessionEvent.payload 필드를 그대로 노출
    * - UI/로그 뷰어에서 raw JSON으로 보거나,
    *   필요 시 클라이언트에서 파싱해 표현 가능
    * */
    private String payload;
    private LocalDateTime createdAt;
    /*
    * 이 이벤트를 발생시킨 유저 ID
    * - 세션과 유저의 관계를 합쳐서 전달할 때 사용
    * - SessionEvent에는 없고, 상위 조회 로직에서 주입해주는 값
    * */
    private Long userId;

    /*
    * SessionEvent 엔티티 + userId 값을 기반으로
    * EventLogDTO 인스턴스를 생성하는 팩토리 메서드
    * - 엔티티를 그대로 노출하지 않고
    *   조회/응답에 적합한 형태로 변환하는 책임을 DTP 쪽에 둠
    * */
    public static EventLogDTO from(SessionEvent e, Long userId) {
        return EventLogDTO.builder()
                .id(e.getId())
                .sessionId(e.getSessionId())
                .seq(e.getSeq())
                .tMs(e.getTMs())
                .type(e.getType())
                .payload(e.getPayload())
                .createdAt(e.getCreatedAt())
                .userId(userId)
                .build();
    }

}
