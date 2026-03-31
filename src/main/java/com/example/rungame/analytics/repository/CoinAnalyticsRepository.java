package com.example.rungame.analytics.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

/*
* 코인 사용량 분석 전용 Repository
*
* - 대용량 집계 쿼리를 효율적으로 수행하기 위해
*   JPA가 아닌 JdbcTemplate 기반으로 구현
*
* 코인 획득/사용 흐름을 분석하여
* 재화 밸런스 및 운영 지표로 활용함
* */
@Repository
public class CoinAnalyticsRepository {

    /*
    * JDBC 템플릿
    *
    * - 복잡한 집계 SQL 실행용
    * */
    private final JdbcTemplate jdbcTemplate;
    public CoinAnalyticsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /*
    * 코인 사용량 요약 통계 조회
    *
    * @return
    * - session_count : 코인 이벤트가 발생한 세션 수
    * - coins_earned : 획득한 코인 수 (coin_pick)
    * - coins_used : 사용한 코인 수 (coin_use)
    * */
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
