package com.example.rungame.session.repository;

import com.example.rungame.event.domain.EventType;
import com.example.rungame.session.domain.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/*
* 게임 세션 저장소(세션 전용 레포지토리)
*
* - 유저의 게임 플레이 기록에 대해 점수,거리,코인의 실시간 업데이트와
*   각종 통계,지표를 조회하는 인터페이스
*
* - 세션 진행 중
*   : 점수,코인,거리 증가, 패널티 반영
* - 유저 개인 통계
*   - 누적 점수,거리,코인 합계
*   - 총 플레이 시간, 최근 플레이 일자
*   - 최고 점수,거리,코인, 평균 기록
* - 서비스 전체 통계
*   - DAU.WAU/MAU 계산
*   - 평균 플레이 시간,점수
*   - 날짜별 세션,점수,코인,거리 집계
* */
public interface SessionRepository extends JpaRepository<Session, Long> {
    //특정 유저의 가장 최근 세션 한 건 조회
    Optional<Session> findTopByUserIdOrderByIdDesc(Long userId);

    //관리자 상세 조회용 -> 특정 유저의 최근 세션 5개
    List<Session> findTop5ByUserIdOrderByStartedAtDesc(Long userId);

    //특정 유저의 전체 세션 수
    long countByUserId(Long userId);

    /*
    * 세션 진행 중 점수,코인 증가
    * - endedAt 이 null인 세션에만 반영
    * - deltaScore, deltaCoins 만큼 누적
    * */
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

    /*
    * 세션 진행 중 거리 + 점수 동시에 증가
    * - reverse 구간처럼 점수는 늘지 않고 거리만 증가해야 하는 경우
    *   deltaScore 를 0으로 호출
    * */
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

    //세션 진행 중 거리만 증가
    @Modifying
    @Query("""
                update Session s
                set s.distance = s.distance + :deltaDist
                where s.id = :sid and s.endedAt is null
            """)
    int incrDistanceOnly(@Param("sid") Long sessionId,
                         @Param("deltaDist") int deltaDistancePx);

    /*
    * 점수 패널티 적용
    * - 현재 점수가 amount보다 작으면 감점하지 않고 유지
    *   -> 0점 아래로 떨어지지 않도로 방어 유도
    * */
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

    /*
    * 유저 전체 세션 기준 총 플레이 시간
    * - DB 함수 TIMESTAMPDIFF 사용
    * */
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

    /*
    * 현재 진행 중인 세션들의 평균 진행 시간
    * - ended_at IS NULL 인 세션만 대상으로
    *   started_at ~ NOW() 의 평균 구간 길이를 계산
    * */
    @Query(value = """
                SELECT COALESCE(AVG(TIMESTAMPDIFF(SECOND, s.started_at, NOW())), 0)
                FROM sessions s
                WHERE s.ended_at IS NULL
            """, nativeQuery = true)
    long avgActiveSecondsForActiveSessions();

    /*
    * 일자별 세션,점수,코인,거리 집계
    * - ended_at 이 존재하는 세션만 대상으로 from~to 사이 날짜별 통계를 한 번에 가져옴
    * */
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

    /*
    * 오늘 이후 시작된 세션 수
    * - from 에 온르 00:00을 넣으면 DAU 계산에 참고 가능
    * */
    long countByStartedAtAfter(LocalDateTime from);

    //특정 시간 내 시작된 세션 수
    long countByStartedAtBetween(LocalDateTime start, LocalDateTime end);

    /*
    * DAU/WAU/MAU 계산용
    * - from 이후에 플레이한 distinct userId 수
    * - from 에 오늘 00:00 -> DAU
    * - from 에 7일 전 00:00 -> 7일 기준 활성 유저 수 등
    * */
    @Query("""
                SELECT count(distinct s.userId)
                FROM Session s
                WHERE s.startedAt >= :from
            """)
    long countDistinctUserIdByStartedAtAfter(@Param("from") LocalDateTime from);

    /*
    * 전체 세션 기준 평균 플레이 시간
    * - 종료된 세션만 대상
    * */
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

    /*
    * 특정 유저의 마지막 플레이 시각
    * - 최근 접속일,마지막 플레이 표시용
    * */
    @Query("SELECT MAX(s.startedAt) FROM Session s WHERE s.userId = :userId")
    LocalDateTime lastPlayedAt(Long userId);

}
