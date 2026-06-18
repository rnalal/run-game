package com.example.rungame.event.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

//개별 게임 세션에서 발생한 이벤트를 기록하는 JPA 엔티티
@Entity
@Table(
        name = "sessions_events",
        uniqueConstraints = @UniqueConstraint(
                name="uq_se_session_seq",
                columnNames = {"session_id", "seq", "t_ms"}
        ),
        indexes = {
                @Index(name = "idx_se_session_type_tms_id", columnList="session_id, type, t_ms DESC, id DESC"),
                @Index(name = "idx_se_type_created", columnList = "type, created_at"),
                @Index(name = "idx_se_created_id", columnList = "created_at DESC, id DESC"),
                @Index(name = "idx_se_session_created_id", columnList = "session_id, created_at DESC, id DESC"),
                @Index(name = "idx_se_type_created_id", columnList = "type, created_at DESC, id DESC")
        }
)
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class SessionEvent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="session_id", nullable = false)
    private Long sessionId;

    //세션 내 이벤트 순번
    @Column(nullable = false)
    private int seq;

    //세션 시작 시점을 기준으로 한 상대 시간
    @Column(name="t_ms", nullable = false)
    private int tMs;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 40, nullable = false)
    private EventType type;

    //이벤트 상세 정보 payload (JSON 문자열)
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
