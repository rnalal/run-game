package com.example.rungame.analytics.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/*
* 이벤트 사용 통계 분석 Repository
*
* - 이벤트 발생 빈도 집계 전용
* - 관리자 분석 대시보드 및 게임 밸런스 분석에 활용
*
* 대량 로그 데이터 집계를 위해
* JdbcTemplate 기반 SQL 실행 방식 사용
* */
@Repository
public class EventAnalyticsRepository {

    /*
    * JDBC 템플릿
    *
    * - 집계 쿼리 전용 실행 도구
    * */
    private final JdbcTemplate jdbcTemplate;
    public EventAnalyticsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /*
    * 이벤트 발생 빈도 상위 목록 조회
    *
    * @return
    * - event_type : 이벤트 타입
    * - count : 발생 횟수
    *
    * 이벤트 타입별 발생 빈도를 기준으로
    * 내림차순 정렬된 결과 반환
    * */
    public List<Map<String, Object>> topEventFrequency() {
        String sql = """
                    SELECT type AS event_type, COUNT(*) AS count
                    FROM sessions_events
                    GROUP BY type
                    ORDER BY count DESC
                """;
        return jdbcTemplate.queryForList(sql);
    }
}
