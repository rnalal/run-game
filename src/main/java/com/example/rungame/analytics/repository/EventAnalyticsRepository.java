package com.example.rungame.analytics.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class EventAnalyticsRepository {

    private final JdbcTemplate jdbcTemplate;
    public EventAnalyticsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    //이벤트 발생 빈도 상위 목록 조회
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
