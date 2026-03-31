package com.example.rungame.system.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/*
* 시스템 상태 점검 서비스
* - 정상 동작 중인지 빠르게 확인할 수 있는 헬스 체크용 서비스
*
* - 시간 반환
*   -appTime
*       -서버에서 현재 시간을 문자열로 내려줌
*       -운영 환경에서 서버 시간,타임존이 정상인지를 확인하는 용도로도 사용
* - DB 연결 상태 확인
*   -health()
*       -JdbcTemplate으로 SELECT 같은 아주 가벼운 쿼리를 실행해서 DB 연결이 가능한지 체크함
*       - 쿼리가 정상 응답이면 UP, 예외가 발생하면 DOWN으로 표시함
*       - 예외 메세지는 dbError 키로 함께 내려줘서 원인 파악에 도움을 줌
*
* */
@Service
@RequiredArgsConstructor
public class SystemService {

    //시스템,DB 상태를 간단히 점검하기 위한 JDBC 접근 도구
    private final JdbcTemplate jdbcTemplate;

    /*
    * 상태 헬스 체크
    *
    * -appTime
    *   -LocalDateTime.now()를 문자열로 담아 현재 서버 시간을 내려줌
    * -db
    *   -SELECT1 쿼리를 실행해서 DB 연결 여부를 확인함
    *       -정상 응답(1) -> UP
    *       -결과가 애매하거나 null -> UNKNOWN
    *       -예외 발생 -> DOWN + dbError에 메세지 추가
    * */
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
