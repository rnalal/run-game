package com.example.rungame.system.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

//시스템 상태 점검 서비스
@Service
@RequiredArgsConstructor
public class SystemService {

    //시스템,DB 상태를 간단히 점검하기 위한 JDBC 접근 도구
    private final JdbcTemplate jdbcTemplate;

    //상태 헬스 체크
    public Map<String, Object> health() {
        Map<String, Object> m = new HashMap<>();
        m.put("appTime", LocalDateTime.now().toString());
        try {
            Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            m.put("db", (one != null && one == 1) ? "UP" : "UNKNOWN");
        } catch (Exception e) {
            m.put("db", "DOWN");
            m.put("dbError", e.getMessage());
        }
        return m;
    }
}
