package com.example.rungame.event.repository;

import com.example.rungame.event.domain.SessionEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SessionEventJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    //이벤트 로그 배치 저장
    public int batchInsertIgnore(List<SessionEvent> events) {

        //저장할 이벤트 없으면 종료
        if (events == null || events.isEmpty()) {
            return 0;
        }

        String sql = """
                INSERT IGNORE INTO sessions_events
                (session_id, seq, t_ms, type, payload, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        LocalDateTime now = LocalDateTime.now();

        int[][] results = jdbcTemplate.batchUpdate(
                sql,
                events,
                100,
                (ps, event) -> {
                    ps.setLong(1, event.getSessionId());
                    ps.setInt(2, event.getSeq());
                    ps.setInt(3, event.getTMs());
                    ps.setString(4, event.getType().name());
                    ps.setString(5, event.getPayload());
                    ps.setTimestamp(6, Timestamp.valueOf(now));
                }
        );

        int inserted = 0;

        //실제 저장된 행 수 계산
        for (int[] batch : results) {
            for (int count : batch) {
                if (count > 0) {
                    inserted += count;
                } else if (count == Statement.SUCCESS_NO_INFO) {
                    inserted++;
                }
            }
        }
        return inserted;
    }
}
