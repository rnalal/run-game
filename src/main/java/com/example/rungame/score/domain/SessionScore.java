package com.example.rungame.score.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="sessions_scores")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class SessionScore {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //원본 세션 ID
    @Column(name="session_id", nullable = false)
    private Long sessionId;

    @Column(name="user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private int score;

    //일 단위 기간 정보
    //하루 기준 랭킹,통계를 뽑을 때 사용
    @Column(name="period_day")
    private LocalDate periodDay; // YYYY-MM-DD

    //주 단위 기간 정보
    @Column(name="period_week")
    private Integer periodWeek; // ISO week

    //월 단위 기간 정보
    @Column(name="period_month")
    private Integer periodMonth; // 1 ~ 12

    //연도 정보
    @Column(name="period_year")
    private Integer periodYear; // 2025

    //생성 시각
    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt;

    //최초 저장 시점에 createdAt 자동 세팅
    @PrePersist
    void prePersist(){
        createdAt = LocalDateTime.now();
    }
}
