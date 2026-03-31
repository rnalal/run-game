package com.example.rungame.session.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/*
* 게임 세션 엔티티
* - 유저가 게임을 한 판 플레이할 때마다 시작,종료 시각, 점수,거리,코인, 장치 정보, 이상 징후 등을
*   한 번에 묶어서 저장하는 게임 한 판 기록
*
* - 리더보드나 통계는 여러 세션을 묶어서 쓰지만
*   기본 단위는 유저가 게임을 한 번 플레이한 기록이어야 함
* - 이 엔티티는 그 한 판에 대한 모든 핵심 정보를 모아 두고
*   나중에 운영,분석,부정 행위 탐지에 활용
*
* - @PrePersist
*   - createdAt/updatedAt 현재 시각으로 초기화
*   - status 기본값을 ACTIVE로 세팅
*   - checksumVAalid 기본 true
* - @PreUpdate
*   - updatedAt만 갱신
* */
@Entity
@Table(name = "sessions")
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

    /*
    * 세션 상태
    * - 세션 시작 시 기본 ACTIVE
    * - 게임 종료 시 ENDED로 변경
    * */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    //세션 시작 시각
    //startSession 호출 시점 기준
    @Column(name="started_at", nullable = false)
    private LocalDateTime startedAt;

    /*
    * 세션 종료 시각
    * - endSession 호출 시점 기준
    * - 아직 끝나지 않은 세션은 null
    * */
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

    /*
    * 클라이언트 데이터,이벤트 체크 검증 결과
    * - true: 기본값, 이상 없음
    * - false: 체크섬,검증 로직에서 문제가 감지된 세션
    * */
    @Column(name="checksum_valid", nullable = false)
    private boolean checksumValid;

    /*
    * 세션 중 감지된 이상 징후 플래그 목록
    * - ABNORMAL_DIRECTION, EVENT_SPAM..
    *
    * 추후 Enum -> 별도 테이블로 분리도 가능하지만 지금은 운영,분석용 태그를
    * 가볍게 붙여두는 용도
    * */
    @Column(length=64)
    private String flags;

    /*
    * 디바이스 정보(JSON 문자열)
    * - 특정 브라우저, 해상도 이슈 분석
    * - 부정행위 패턴 탐지
    * */
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
