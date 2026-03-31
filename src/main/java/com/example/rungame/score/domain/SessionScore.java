package com.example.rungame.score.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/*
* 세션 점수 엔티티
* - 한 번 끝난 게임 세션의 점수를 일,주,월,연 단위로 묶어서 리더보드,통계에 쓰기 좋게
*   저장해 두는 기록 테이블
*
* - Session 엔티티에 모든 점수 집계를 걸어두면 랭킹이나 통계 쿼리가 점점 무거워짐
* - 그래서 세션이 끝날 때마다 그때의 점수와 기간 정보를 따로 떼어 저장해 두고
*   리더보드,통계는 이 테이블 기준으로 조회
* */
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

    /*
    * 생성 시각
    * - 세션 종료 후 이 레코드가 실제로 기록된 시각
    * - period_* 필드는 게임 날짜 기준이고 createdAt은 데이터가 적재된 시간을 나타냄
    * */
    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt;

    //최초 저장 시점에 createdAt 자동 세팅
    @PrePersist
    void prePersist(){
        createdAt = LocalDateTime.now();
    }
}
