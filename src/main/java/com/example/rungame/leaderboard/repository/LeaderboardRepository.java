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

public interface LeaderboardRepository extends JpaRepository<Session, Long> {

    //전체 리더보드 조회
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

    //최근 N일 기준 리더보드 조회
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

    //특정 유저의 전체 기간 기준 최고 점수 조회
    @Query("""
        SELECT MAX(s.score)
        FROM Session s
        WHERE s.userId = :userId
          AND s.status = 'ENDED'
    """)
    Integer findUserBestScore(@Param("userId") Long userId);

    //전체 기간 순위 계산 쿼리
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

    //해당 유저가 세션을 가진 적이 있는지 여부
    @Query("""
                SELECT COUNT(s)
                FROM Session s
                WHERE s.userId = :userId
            """)
    long countAllSessionsByUser(@Param("userId") Long userId);

    //특정 유저의 순위 계산
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

    //기간 필터 기준: 유저별 최고 점수 목록
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

    //기간 필터 기준: 참여 유저 수
    @Query("""
                SELECT COUNT(DISTINCT s.userId)
                FROM Session s
                WHERE s.status = com.example.rungame.session.domain.Session.Status.ENDED
                    AND (:startDate IS NULL OR s.endedAt >= :startDate)
            """)
    long countDistinctUsersWithEnded(@Param("startDate") LocalDateTime startDate);

    //기간 필터 기준: 최고 점수 조회
    @Query("""
               SELECT COALESCE(MAX(s.score), 0)
               FROM Session s
               WHERE s.status = com.example.rungame.session.domain.Session.Status.ENDED
                AND (:startDate IS NULL OR s.endedAt >= :startDate) 
           """)
    Integer maxScoreInRange(@Param("startDate") LocalDateTime startDate);

}
