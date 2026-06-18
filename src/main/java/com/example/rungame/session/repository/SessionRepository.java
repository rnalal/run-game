package com.example.rungame.session.repository;

import com.example.rungame.session.domain.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long>,
        JpaSpecificationExecutor<Session> {

    interface DailySessionCount {
        LocalDate getDay();
        Long getSessionCount();
    }

    //특정 유저의 가장 최근 세션 한 건 조회
    Optional<Session> findTopByUserIdOrderByIdDesc(Long userId);

    //관리자 상세 조회용 -> 특정 유저의 최근 세션 5개
    List<Session> findTop5ByUserIdOrderByStartedAtDesc(Long userId);

    //특정 유저의 전체 세션 수
    long countByUserId(Long userId);

    //활성 세션 종료 처리
    @Modifying
    @Query("""
        update Session s
        set s.status = com.example.rungame.session.domain.Session.Status.ENDED,
            s.endedAt = :endedAt
        where s.id = :sessionId
          and s.userId = :userId
          and s.endedAt is null
    """)
    int endActiveSession(
            @Param("sessionId") Long sessionId,
            @Param("userId") Long userId,
            @Param("endedAt") LocalDateTime endedAt
    );

    //세션 진행 중 점수,코인 증가
    @Modifying
    @Query("""
                update Session s 
                set s.score = s.score + :deltaScore,
                    s.coins = s.coins + :deltaCoins
                where s.id = :sid and s.endedAt is null
            """)
    int incrScoreAndCoins(@Param("sid") Long sessionId,
                          @Param("deltaScore") int deltaScore,
                          @Param("deltaCoins") int deltaCoins);

    //세션 진행 중 거리 + 점수 동시에 증가
    @Modifying
    @Query("""
                update Session s
                set s.distance = s.distance + :deltaDist,
                    s.score = s.score + :deltaScore
                where s.id = :sid and s.endedAt is null
            """)
    int incrDistanceAndScore(@Param("sid") Long sessionId,
                             @Param("deltaDist") int deltaDistancePx,
                             @Param("deltaScore") int deltaScore);

    //세션 진행 중 거리,점수,코인을 한 번에 누적
    @Modifying
    @Query("""
                update Session s
                set s.distance = s.distance + :deltaDist,
                    s.score = s.score + :deltaScore,
                    s.coins = s.coins + :deltaCoins
                where s.id = :sid and s.endedAt is null
            """)
    int incrDistanceScoreAndCoins(@Param("sid") Long sessionId,
                                  @Param("deltaDist") int deltaDistancePx,
                                  @Param("deltaScore") int deltaScore,
                                  @Param("deltaCoins") int deltaCoins);

    //세션 진행 중 거리만 증가
    @Modifying
    @Query("""
                update Session s
                set s.distance = s.distance + :deltaDist
                where s.id = :sid and s.endedAt is null
            """)
    int incrDistanceOnly(@Param("sid") Long sessionId,
                         @Param("deltaDist") int deltaDistancePx);

    //점수 패널티 적용
    @Modifying
    @Query("""
                update Session s
                set s.score = case
                    when s.score >= :amount then s.score - :amount
                    else s.score
                end
                where s.id = :sid and s.endedAt is null
            """)
    int penalizeScoreWithFloor(@Param("sid") Long sessionId,
                               @Param("amount") int amount);

    //유저 전체 세션 기준 누적 점수
    @Query("select coalesce(sum(s.score),0) from Session s where s.userId = :userId")
    long sumScoreByUserId(Long userId);

    //유저 전체 세션 기준 누적 처리
    @Query("select coalesce(sum(s.distance), 0) from Session s where s.userId = :userId")
    long sumDistanceByUserId(Long userId);

    //유저 전체 세션 기준 누적 코인
    @Query("select coalesce(sum(s.coins), 0) from Session s where s.userId = :userId")
    long sumCoinsByUserId(Long userId);

    //유저 전체 세션 기준 총 플레이 시간
    @Query(value = """
                        SELECT COALESCE(SUM(TIMESTAMPDIFF(SECOND, s.started_at, s.ended_at)), 0)
                        FROM sessions s
                        WHERE s.user_id = :userId
                    """, nativeQuery = true)
    long sumPlaySecondsByUserId(@Param("userId") Long userId);

    //최근 N일 내 플레이 횟수
    long countByUserIdAndStartedAtBetween(Long userId, LocalDateTime from, LocalDateTime to);

    //특정 기간 내 플레이 시간 합계
    //- from ~ to 구간에 시작된 세션만 대상으로 합산
    @Query(value = """
                        SELECT COALESCE(SUM(TIMESTAMPDIFF(SECOND, s.started_at, s.ended_at)), 0)
                        FROM sessions s
                        WHERE s.user_id = :userId AND s.started_at BETWEEN :from AND :to
                    """, nativeQuery = true)
    long sumPlaySecondsByUserIdInRange(@Param("userId") Long userId,
                                       @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to);

    //세션 상태별 개수
    long countByStatus(Session.Status status);

    //현재 진행 중인 세션들의 평균 진행 시간
    @Query(value = """
                SELECT COALESCE(AVG(TIMESTAMPDIFF(SECOND, s.started_at, NOW())), 0)
                FROM sessions s
                WHERE s.ended_at IS NULL
            """, nativeQuery = true)
    long avgActiveSecondsForActiveSessions();

    //일자별 세션,점수,코인,거리 집계
    @Query(value= """
                SELECT
                    DATE(s.ended_at) AS day,
                    COUNT(*) AS session_cnt,
                    COALESCE(AVG(s.score),0) AS avg_score,
                    COALESCE(AVG(s.coins),0) As avg_coins,
                    COALESCE(AVG(s.distance),0) AS avg_distance
                FROM sessions s
                WHERE s.ended_at IS NOT NULL
                    AND DATE(s.ended_at) BETWEEN :from AND :to
                GROUP BY DATE(s.ended_at)
                ORDER BY DATE(s.ended_at)
            """, nativeQuery = true)
    List<Map<String, Object>> aggregateDailyStats(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    //오늘 이후 시작된 세션 수
    long countByStartedAtAfter(LocalDateTime from);

    //특정 시간 내 시작된 세션 수
    long countByStartedAtBetween(LocalDateTime start, LocalDateTime end);

    //관리자 대시보드 최근 N일 세션 수 차트용
    @Query(value = """
                SELECT DATE(s.started_at) AS day,
                       COUNT(*) AS sessionCount
                FROM sessions s
                WHERE s.started_at >= :from
                    AND s.started_at < :to
                GROUP BY DATE(s.started_at)
                ORDER BY DATE(s.started_at)
            """, nativeQuery = true)
    List<DailySessionCount> countDailySessions(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    //DAU/WAU/MAU 계산용
    @Query("""
                SELECT count(distinct s.userId)
                FROM Session s
                WHERE s.startedAt >= :from
            """)
    long countDistinctUserIdByStartedAtAfter(@Param("from") LocalDateTime from);

    //전체 세션 기준 평균 플레이 시간
    @Query(value = """
                SELECT COALESCE(AVG(TIMESTAMPDIFF(SECOND, s.started_at, s.ended_at)), 0)
                FROM sessions s
                WHERE s.ended_at IS NOT NULL
            """, nativeQuery = true)
    long avgPlaySeconds();

    //전체 세션 기준 평균 점수
    @Query("SELECT COALESCE(AVG(s.score), 0) FROM Session s WHERE s.endedAt IS NOT NULL")
    long avgScore();

    //특정 유저의 최고 점수
    @Query("SELECT COALESCE(MAX(s.score), 0) FROM Session s WHERE s.userId = :userId")
    long bestScoreByUserId(Long userId);

    //특정 유저의 최고 거리
    @Query("SELECT COALESCE(MAX(s.distance), 0) FROM Session s WHERE s.userId = :userId")
    long bestDistanceByUserId(Long userId);

    //특정 유저의 최고 코인 수
    @Query("SELECT COALESCE(MAX(s.coins), 0) FROM Session s WHERE s.userId = :userId")
    long bestCoinsByUserId(Long userId);

    //특정 유저의 평균 점수
    @Query("SELECT COALESCE(AVG(s.score), 0) FROM Session s WHERE s.userId = :userId")
    double avgScoreByUserId(Long userId);

    //특정 유저의 평균 거리
    @Query("SELECT COALESCE(AVG(s.distance), 0) FROM Session s WHERE s.userId = :userId")
    double avgDistanceByUserId(Long userId);

    //특정 유저의 마지막 플레이 시각
    @Query("SELECT MAX(s.startedAt) FROM Session s WHERE s.userId = :userId")
    LocalDateTime lastPlayedAt(Long userId);

}
