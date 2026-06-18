package com.example.rungame.analytics.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class UserAnalyticsRepository {

    private final JdbcTemplate jdbcTemplate;
    public UserAnalyticsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    //주간 신규 사용자 증가 통계 조회
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
