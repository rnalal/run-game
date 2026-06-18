package com.example.rungame.analytics.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public class CoinAnalyticsRepository {

    //JDBC 템플릿
    private final JdbcTemplate jdbcTemplate;
    public CoinAnalyticsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    //코인 사용량 요약 통계 조회
    public Map<String, Object> coinUsagesSummary() {
        String sql = """
                    SELECT
                         COUNT(DISTINCT session_id) AS session_count,
                         SUM(CASE WHEN type='coin_pick' THEN 1 ELSE 0 END) AS coins_earned,
                         SUM(CASE WHEN type='coin_use' THEN 1 ELSE 0 END) AS coins_used
                    FROM sessions_events
                """;
        return jdbcTemplate.queryForMap(sql);
    }
}
