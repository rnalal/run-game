package com.example.rungame.leaderboard.repository;

import com.example.rungame.leaderboard.dto.LeaderboardEntryDTO;
import com.example.rungame.session.domain.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/*
* 리더보드 조회용 JPA 리포지토리
* - 세션 데이터를 기준으로 유저별 최고 기록을 집계해서 리더보드 DTO로 바로 조회
* - 전체/기간별 리더보드, 유저별 최고 점수, 유저 순위, 통계용 데이터까지 랭킹 관련
*   쿼리를 한 곳에 모아둔 인터페이스
*
* - 유저별 최고 기록 기반 리더보드 페이지 조회
* - 특정 유저의 최고 점수/ 순위 계산
* - 리더보드 요약 통계 제공
* */
public interface LeaderboardRepository extends JpaRepository<Session, Long> {

    /*
    * 전체 리더보드 조회
    * - 유저별 session을 모아서
    *   최고점수, 최고 거리, 최고 코인을 구한 뒤
    *   leaderboardEntryDTO로 바로 매핑
    * */
    @Query("""
        SELECT new com.example.rungame.leaderboard.dto.LeaderboardEntryDTO(
            u.id,
            u.nickname,
            MAX(s.score),
            MAX(s.distance),
            MAX(s.coins),
            0L
        )
        FROM Session s
        JOIN com.example.rungame.user.domain.User u ON s.userId = u.id
        WHERE s.status = 'ENDED'
        GROUP BY u.id, u.nickname
        ORDER BY
            CASE
                WHEN :type = 'score' THEN MAX(s.score)
                WHEN :type = 'distance' THEN MAX(s.distance)
                WHEN :type = 'coins' THEN MAX(s.coins)
                ELSE MAX(s.score)
            END DESC,
            u.nickname ASC
    """)
    Page<LeaderboardEntryDTO> findLeaderboard(
            @Param("type") String type,
            Pageable pageable
    );

    /*
    * 최근 N일 기준 리더보드 조회
    * - endedAt >= startDate 조건으로 기간 필터만 추가된 버전
    * - 나머지는 findLeaderboard와 동일하게
    *   유저별 최고 기록을 LeaderboardEntryDTO로 반환
    * */
    @Query("""
        SELECT new com.example.rungame.leaderboard.dto.LeaderboardEntryDTO(
            u.id,
            u.nickname,
            MAX(s.score),
            MAX(s.distance),
            MAX(s.coins),
            0L
        )
        FROM Session s
        JOIN com.example.rungame.user.domain.User u ON s.userId = u.id
        WHERE s.status = 'ENDED'
          AND s.endedAt >= :startDate
        GROUP BY u.id, u.nickname
        ORDER BY
            CASE
                WHEN :type = 'score' THEN MAX(s.score)
                WHEN :type = 'distance' THEN MAX(s.distance)
                WHEN :type = 'coins' THEN MAX(s.coins)
            END DESC,
            u.nickname ASC
    """)
    Page<LeaderboardEntryDTO> findLeaderboardInRange(
            @Param("type") String type,
            @Param("startDate") LocalDateTime startDate,
            Pageable pageable
    );

    /*
    * 특정 유저의 전체 기간 기준 최고 점수 조회
    * - 리더보드 순위 계산, 마이페이지 최고 점수 표시 등에 활용
    * */
    @Query("""
        SELECT MAX(s.score)
        FROM Session s
        WHERE s.userId = :userId
          AND s.status = 'ENDED'
    """)
    Integer findUserBestScore(@Param("userId") Long userId);

    /*
    * 전체 기간 순위 계산 쿼리
    * - 주어진 userId의 최고 점수보다 더 높은 점수를 가진 유저 수를 세고 + 1
    *   -> 순위를 계산
    * - 아래 findUserRankByScore와 유사한 역할
    *   (COALESCE와 Status enum을 사용하는 버전이 findUserRankByScore)
    * */
    @Query("""
        SELECT COUNT(DISTINCT u2.id) + 1
        FROM Session s2
        JOIN com.example.rungame.user.domain.User u2 ON s2.userId = u2.id
        WHERE s2.status = 'ENDED'
          AND (
              (SELECT MAX(s3.score)
               FROM Session s3
               WHERE s3.userId = :userId
                 AND s3.status = 'ENDED'
              )
              <
              (SELECT MAX(s4.score)
               FROM Session s4
               WHERE s4.userId = u2.id
                 AND s4.status = 'ENDED'
              )
          )
    """)
    Long findUserRank(@Param("userId") Long userId);

    /*
    * 해당 유저가 세션을 가진 적이 있는지 여부
    * - 리더보드/랭킹 조회 전에 이 유저는 게임을 실제로 해본 적이 있는가? 같은 체크에 활용 가능
    * */
    @Query("""
                SELECT COUNT(s)
                FROM Session s
                WHERE s.userId = :userId
            """)
    long countAllSessionsByUser(@Param("userId") Long userId);

    /*
    * 특정 유저의 순위 계산
    * - Status enum을 그대로 사용하고 COALESCE로 NULL인 경우 -1 처리
    * - 기준: 나보다 최고 점수가 높은 유저 수 + 1 = 나의 순위
    * */
    @Query("""
                SELECT COUNT(DISTINCT u2.id) + 1
                FROM Session s2
                JOIN com.example.rungame.user.domain.User u2 ON s2.userId = u2.id
                WHERE s2.status = com.example.rungame.session.domain.Session.Status.ENDED
                  AND COALESCE((
                    SELECT MAX(s3.score)
                    FROM Session s3
                    WHERE s3.userId = :userId
                      AND s3.status = com.example.rungame.session.domain.Session.Status.ENDED
                  ), -1) < COALESCE((
                    SELECT MAX(s4.score)
                    FROM Session s4
                    WHERE s4.userId = u2.id
                      AND s4.status = com.example.rungame.session.domain.Session.Status.ENDED
                  ), -1)
            """)
    Long findUserRankByScore(@Param("userId") Long userId);

    /*
    * 기간 필터 기준: 유저별 최고 점수 목록
    * - startDate 이후의 END 상태 세션만 대상으로 유저별 최고 score/distance/coins를 LeaderboardEntryDTO로 조회
    * - ORDER BY MAX(s.score) DESC
    *   -> 상위 유저부터 순서대로 나열
    * - 리더보드 요약 통계를 서비스 계층에서 계산할 때 기초 데이터로 활용
    * */
    @Query("""
                SELECT new com.example.rungame.leaderboard.dto.LeaderboardEntryDTO( 
                        u.id,
                        u.nickname, 
                        MAX(s.score), 
                        MAX(s.distance), 
                        MAX(s.coins), 
                        0L 
                ) 
                FROM Session s 
                JOIN com.example.rungame.user.domain.User u ON s.userId = u.id 
                WHERE s.status = com.example.rungame.session.domain.Session.Status.ENDED 
                AND (:startDate IS NULL OR s.endedAt >= :startDate) 
                GROUP BY u.id, u.nickname 
                ORDER BY MAX(s.score) DESC
            """)
    List<LeaderboardEntryDTO> findAllBestScoresDesc(@Param("startDate") LocalDateTime startDate);

    /*
    * 기간 필터 기준: 참여 유저 수
    * - END 상태 세션을 가진 distinct userId 개수
    * - 리더보드/통계 상단에 이번 기간 중 총 -명 참여 같은 숫자를 보여줄 때 사용
    * */
    @Query("""
                SELECT COUNT(DISTINCT s.userId)
                FROM Session s
                WHERE s.status = com.example.rungame.session.domain.Session.Status.ENDED
                    AND (:startDate IS NULL OR s.endedAt >= :startDate)
            """)
    long countDistinctUsersWithEnded(@Param("startDate") LocalDateTime startDate);

    /*
    * 기간 필터 기준: 최고 점수 조회
    * - END 상태 세션 + 시작일자 조건으로 전체 최고 score를 구함
    * - 기록이 아예 없을 경우 0으로 반환
    * */
    @Query("""
               SELECT COALESCE(MAX(s.score), 0)
               FROM Session s
               WHERE s.status = com.example.rungame.session.domain.Session.Status.ENDED
                AND (:startDate IS NULL OR s.endedAt >= :startDate) 
           """)
    Integer maxScoreInRange(@Param("startDate") LocalDateTime startDate);

}
