package com.example.rungame.audit.aop;

import com.example.rungame.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/*
* 관리자 행위 감사 AOP
*
* - @AdminAction 이 붙은 메서드 실행 후
*   관리자 행위를 자동으로 감사 로그로 기록
*
* 비즈니스 로직과 감사 로직을 분리해서
* 코드 중복 없이 운영 이력을 추적할 수 있음
* */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    //감사 로그 기록 서비스
    private final AuditLogService auditLogService;

    /*
    * @AdminAction 어노테이션이 붙은 메서드가
    * 정상적으로 반환된 이후 실행됨
    * */
    @AfterReturning("@annotation(adminAction)")
    public void after(JoinPoint jp, AdminAction adminAction) {
        //관리자 식별
        String actor = resolveActor();
        //어노테이션에 정의된 활동 정보
        String action = adminAction.value();
        String resource = adminAction.resource();

        //메서드 정보 기반 상세 설명
        String details = "method=" + jp.getSignature().toShortString();
        //감사 로그 기록
        auditLogService.write(actor, action, resource, details);
        //서버 로그에도 출력
        log.info("[AUDIT] actor={} action={} resource={} details={}",
                actor, action, resource, details);
    }

    /*
    * 현재 인증된 관리자 식별
    *
    * @return : 관리자 식별
    * */
    private String resolveActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return "anonymous";

        Object principal = auth.getPrincipal();
        return principal == null ? "anonymous" : principal.toString();
    }
}
