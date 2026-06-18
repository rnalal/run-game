package com.example.rungame.session.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "sessions",
        indexes = {
                @Index(name = "fk_sessions_user", columnList="user_id"),
                @Index(name = "idx_sessions_started_at", columnList = "started_at"),
                @Index(name = "idx_sessions_started_user", columnList = "started_at, user_id"),
                @Index(name = "idx_sessions_status", columnList = "status"),
                @Index(name = "idx_sessions_ended_at", columnList = "ended_at")
        }
)
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Session {

    //세션 상태
    public enum Status { ACTIVE, ENDED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable = false)
    private Long userId;

    //세션 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    //세션 시작 시각
    //startSession 호출 시점 기준
    @Column(name="started_at", nullable = false)
    private LocalDateTime startedAt;

    //세션 종료 시각
    @Column(name="ended_at")
    private LocalDateTime endedAt;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private int distance;

    //세션 동안 획득한 코인 수
    @Column(nullable = false)
    private int coins;

    @Column(name = "max_speed", precision=6, scale=2)
    private BigDecimal maxSpeed;

    //클라이언트 데이터,이벤트 체크 검증 결과
    @Column(name="checksum_valid", nullable = false)
    private boolean checksumValid;

    //세션 중 감지된 이상 징후 플래그 목록
    @Column(length=64)
    private String flags;

    //디바이스 정보(JSON 문자열)
    @Lob
    @Column(name="device_info")
    private String deviceInfo;

    //레코드 생성 시각
    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt;

    //레코드 마지막 수정 시각
    @Column(name="updated_at", nullable = false)
    private LocalDateTime updatedAt;

    //최초 저장 시 공통 초기값 세팅
    @PrePersist
    void prePersist(){
        createdAt = updatedAt = LocalDateTime.now();
        checksumValid = true;
        if(status == null) status = Status.ACTIVE;
    }

    //수정 시점마다 updatedAt 갱신
    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
