package com.example.rungame.system.maintenance;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

//시스템 유지보수용 서비스
@Service
@RequiredArgsConstructor
public class MaintenanceService {

    //관리용 SQL을 실행하기 위한 JdbcTemplate
    private final JdbcTemplate jdbcTemplate;

    //오래된 세션 이벤트 로그 정리
    public int purgeOldEvents(int days) {
        try {
            String sql = "DELETE FROM sessions_events WHERE created_at < DATE_SUB(NOW(), INTERVAL ? DAY)";
            return jdbcTemplate.update(sql, days);
        } catch (Exception e) {
            System.err.println("⚠ purgeOldEvents 실패: " + e.getMessage());
            return 0;
        }
    }

    //오래된 관리자,운영 로그 정리
    public int purgeOldAuditLogs(int days) {
        try {
            String sql = "DELETE FROM admin_logs WHERE created_at < DATE_SUB(NOW(), INTERVAL ? DAY)";
            return jdbcTemplate.update(sql, days);
        } catch (Exception e) {
            System.err.println("⚠ purgeOldAuditLogs 실패: " + e.getMessage());
            return 0;
        }
    }

    //테이블 데이터를 CSV 형태로 백업하기 위한 골격 메서드
    public String backupTableToCsv(String table, String outPath) {
        String sql = "SELECT * FROM " + table;
        return outPath;
    }
}
