package com.example.rungame.system.maintenance;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/*
* 시스템 유지보수용 서비스
* - 운영 중 쌓이는 로그,이벤트 데이터를 주기적으로 삭제하거나 백업할 수 있는 유지보수 전용 서비스
*
* - 오래된 이벤트 로그 정리
*   - purgeOleEvents(int days)
*       - 세션 이벤트 테이블에서 기준 일수보다 오래된 행들을 삭제
* - 오래된 관리자,운영 로그 정리
*   - purgeOldAuditLogs(int days)
*       - 관리자 로그 테이블에서 기준 일수보다 오래된 행들을 삭제
* - 테이블 CSV 백업
*   - backupTableToCsv(String table, String outPath)
*       - 전달받은 테이블 이름을 대상으로 SELECT * 쿼리를 만들고 지정된 경로에 CSV 형태로 덤프하는 기능을
*         확장할 수 있도록 마련해 둔 메서드
*       - 현재는 쿼리 문자열만 구성하고 실제 파일로 쓰는 로직은 추후 구현 대상임
* */
@Service
@RequiredArgsConstructor
public class MaintenanceService {

    //관리용 SQL을 실행하기 위한 JdbcTemplate
    private final JdbcTemplate jdbcTemplate;

    /*
    * 오래된 세션 이벤트 로그 정리
    *
    * @param days: 보존할 기간(일)
    * @return : 삭제된 행 개수
    *
    * - sessions_events 테이블에서 created_at 기준으로 NOW() - days 일보다 이전인 데이터만 삭제
    * - 예외 발생 시 0을 반환하고 표준 에러로 간단한 메세지는 남김
    * */
    public int purgeOldEvents(int days) {
        try {
            String sql = "DELETE FROM sessions_events WHERE created_at < DATE_SUB(NOW(), INTERVAL ? DAY)";
            return jdbcTemplate.update(sql, days);
        } catch (Exception e) {
            System.err.println("⚠ purgeOldEvents 실패: " + e.getMessage());
            return 0;
        }
    }

    /*
    * 오래된 관리자,운영 로그 정리
    *
    * @param days : 보존할 기간(일)
    * @return : 삭제된 행 개수
    *
    * - admin_logs 테이블에서 created_at 기준으로 NOW() - days 일보다 이전인 데이터만 삭제함
    * - 예외 발생 시 0을 반환하고 표준 에러로 간단한 메세지를 남김
    * */
    public int purgeOldAuditLogs(int days) {
        try {
            String sql = "DELETE FROM admin_logs WHERE created_at < DATE_SUB(NOW(), INTERVAL ? DAY)";
            return jdbcTemplate.update(sql, days);
        } catch (Exception e) {
            System.err.println("⚠ purgeOldAuditLogs 실패: " + e.getMessage());
            return 0;
        }
    }

    /*
    * 테이블 데이터를 CSV 형태로 백업하기 위한 골격 메서드
    *
    * - SELECT * FROM {table} 쿼리 문자열만 구성하고 실제로 결과를 조회해서 outPath 경로에 파일로 저장하는 부분은 추후 구현 예정
    *
    * @param table : 백업 대상 테이블명
    * @param outPath : CSV 파일을 저장할 서버 로컬 경로
    * @return : 지정된 출력 경로 그대로 반환
    * */
    public String backupTableToCsv(String table, String outPath) {
        String sql = "SELECT * FROM " + table;
        return outPath;
    }
}
