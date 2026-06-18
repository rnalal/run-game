package com.example.rungame.event.dto;

import com.example.rungame.event.domain.EventType;
import com.example.rungame.event.domain.SessionEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

//세션 이벤트 로그 조회용 DTO
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
    //이벤트 payload (JSON 문자열)
    private String payload;
    private LocalDateTime createdAt;
    private Long userId;

    //SessionEvent 엔티티 + userId 값을 기반으로
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
