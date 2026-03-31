package com.example.rungame.score.repository;

import com.example.rungame.score.domain.SessionScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/*
* 세션 점수 조회,정리용 레포지토리
* - SessionScore 엔티티에 대해 일,주,월 기준 상위 랭킹 조회와
*   기간별 정리를 담당하는 저장소 인터페이스
*
* - 리더보드,통계 조회는 가급적 SessionScore만 조회해서 처리하고
*   오래된 스냅샷은 배치,스케줄러에서 주기적으로 정리할 수 있도록 분리
* */

public interface SessionScoreRepository extends JpaRepository<SessionScore, Long> {

    /*
    * 특정 날짜의 일간 TOP 50 점수 조회
    *
    * @param day: 기준 일자
    * @return: 해당 일자에 기록된 점수 중 상위 50개
    * */
    List<SessionScore> findTop50ByPeriodDayOrderByScoreDesc(LocalDate day);

    /*
    * 특정 ISO 주차의 주간 TOP 50 점수 조회
    *
    * @param isoWeek: ISO 기준 주차 번호
    * @return: 해당 주차에 기록된 점수 중 상위 50개
    * */
    List<SessionScore> findTop50ByPeriodWeekOrderByScoreDesc(Integer isoWeek);

    /*
    * 특정 연,월의 월간 TOP 50 점수 조회
    *
    * @param year: 연도
    * @param month: 월
    * @return: 해당 월에 기록된 점수 중 상위 50개
    * */
    List<SessionScore> findTop50ByPeriodYearAndPeriodMonthOrderByScoreDesc(Integer year, Integer month);

    /*
    * 일간 기록 정리
    *
    * @param fromDay: 이 날짜 이상인 데이터 삭제
    * @return: 삭제된 로우 수
    * */
    @Modifying
    @Query("DELETE FROM SessionScore s WHERE s.periodDay IS NOT NULL AND s.periodDay >= :fromDay")
    int deleteDailyFrom(@Param("fromDay") java.time.LocalDate fromDay);

    /*
    * 주간 기록 정리
    *
    * @param fromWeek : 이 주차 이상인 데이터 삭제
    * @return : 삭제된 로우 수
    * */
    @Modifying
    @Query("DELETE FROM SessionScore s WHERE s.periodWeek >= :fromWeek")
    int deleteWeeklyFrom(@Param("fromWeek") Integer fromWeek);

    /*
    * 월간 기록 정리
    *
    * @param year : 대상 연도
    * @param fromMonth : 이 월 이상인 데이터 삭제
    * @return: 삭제된 로우 수
    * */
    @Modifying
    @Query("DELETE FROM SessionScore s WHERE s.periodYear = :year AND s.periodMonth >= :fromMonth")
    int deleteMonthlyFrom(@Param("year") Integer year, @Param("fromMonth") Integer fromMonth);
}

