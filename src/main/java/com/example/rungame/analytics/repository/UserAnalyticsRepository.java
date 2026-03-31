package com.example.rungame.analytics.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/*
 * 사용자 성장 분석 Repository
 *
 * - 사용자 가입 추이를 주간 단위로 집계
 * - 관리자 분석 대시보드의 성장 차트 데이터 제공
 *
 * 대량 데이터 집계를 위해
 * JdbcTemplate 기반 SQL을 사용
 */
@Repository
public class UserAnalyticsRepository {

    /*
     * JDBC 템플릿
     *
     * - 집계/통계 SQL 실행 전용
     */
    private final JdbcTemplate jdbcTemplate;
    public UserAnalyticsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /*
     * 주간 신규 사용자 증가 통계 조회
     *
     * @return
     * - week       : 연-주차 (YYYY-WW)
     * - new_users : 해당 주에 가입한 사용자 수
     *
     * 최근 10주 기준으로
     * 사용자 성장 추이를 확인할 수 있음
     */
    public List<Map<String, Object>> weeklyUserGrowth() {
        String sql = """
            SELECT DATE_FORMAT(created_at, '%Y-%u') AS week,
                   COUNT(*) AS new_users
            FROM users
            GROUP BY DATE_FORMAT(created_at, '%Y-%u')
            ORDER BY week DESC
            LIMIT 10
        """;
        return jdbcTemplate.queryForList(sql);
    }
}
