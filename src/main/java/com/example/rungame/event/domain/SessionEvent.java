package com.example.rungame.event.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/*
* 개별 게임 세션에서 발생한 이벤트를 기록하는 JPA 엔티티
*
* - 한 세션 안에서 시간 순서대로 발생한 이벤트 로그를 저장
* - 리플레이, 통계, 디버깅, 운영 모니터링을 위한 핵심 데이터 소스
*
* - session_id + seq + t_ms 에 Unique 제약을 걸어서
*   한 세션 내 동일 시각/순번의 중복 이벤트를 방지
* - payload는 JSON 문자열로 저장해 이벤트별 확장 가능한 데이터 구조를 지원
* */
@Entity
@Table(
        name = "sessions_events",
        uniqueConstraints = @UniqueConstraint(name="uq_se_session_seq", columnNames = {"session_id", "seq", "t_ms"})
)
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class SessionEvent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="session_id", nullable = false)
    private Long sessionId;

    /*
    * 세션 내 이벤트 순번
    * - 클라이언트에서 증가시키며 전송하는 seq 값
    * - 같은 tMs라도 seq로 안정적인 순서 보장 가능
    * */
    @Column(nullable = false)
    private int seq;

    /*
    * 세션 시작 시점을 기준으로 한 상대 시간
    * - 클라이언트에서 nowMs() 기반으로 전송
    * */
    @Column(name="t_ms", nullable = false)
    private int tMs;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 40, nullable = false)
    private EventType type;

    // 이벤트 상세 정보 payload (JSON 문자열)
    @Lob
    @Column
    private String payload;

    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
