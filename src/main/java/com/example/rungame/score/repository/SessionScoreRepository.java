package com.example.rungame.score.repository;

import com.example.rungame.score.domain.SessionScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SessionScoreRepository extends JpaRepository<SessionScore, Long> {

    //특정 날짜의 일간 TOP 50 점수 조회
    List<SessionScore> findTop50ByPeriodDayOrderByScoreDesc(LocalDate day);

    //특정 ISO 주차의 주간 TOP 50 점수 조회
    List<SessionScore> findTop50ByPeriodWeekOrderByScoreDesc(Integer isoWeek);

    //특정 연,월의 월간 TOP 50 점수 조회
    List<SessionScore> findTop50ByPeriodYearAndPeriodMonthOrderByScoreDesc(Integer year, Integer month);

    //일간 기록 정리
    @Modifying
    @Query("DELETE FROM SessionScore s WHERE s.periodDay IS NOT NULL AND s.periodDay >= :fromDay")
    int deleteDailyFrom(@Param("fromDay") java.time.LocalDate fromDay);

    //주간 기록 정리
    @Modifying
    @Query("DELETE FROM SessionScore s WHERE s.periodWeek >= :fromWeek")
    int deleteWeeklyFrom(@Param("fromWeek") Integer fromWeek);

    //월간 기록 정리
    @Modifying
    @Query("DELETE FROM SessionScore s WHERE s.periodYear = :year AND s.periodMonth >= :fromMonth")
    int deleteMonthlyFrom(@Param("year") Integer year, @Param("fromMonth") Integer fromMonth);
}

