package com.example.rungame.audit.service;

import com.example.rungame.audit.domain.AuditLog;
import com.example.rungame.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/*
* 관리자 감사 로그 기록 서비스
*
* - 관리자 행위 정보를 DB에 저장하는 책임을 담당
* - AOP에서 호출되어 실제 로그 영속화를 수행
*
* 감사 로직을 한 곳에 모아서
* 나중에 저장 방식 변경(DB -> 메시지 큐, 외부 로그 시스템 등)에 대비한 구조
* */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    //감사 로그 Repository
    private final AuditLogRepository repo;

    /*
    * 감사 로그 기록
    *
    * @param actor : 관리자 ID, 이메일
    * @param action : 수행한 활동명
    * @param resource : 대상 리소스 식별자
    * @param details : 상세 정보
    * */
    public void write(String actor, String action, String resource, String details){

        //null 방어 처리 (로그 안정성 확보)
        if(actor == null) actor = "unknown";
        if(action == null) action = "";
        if(resource == null) resource = "";
        if(details == null) details = "";

        //감사 로그 엔티티 생성
        AuditLog log = AuditLog.builder()
                .actor(actor)
                .action(action)
                .resource(resource)
                .details(details)
                .build();
        //영속화
        repo.save(log);
    }
}
