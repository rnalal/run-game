package com.example.rungame.system.maintenance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

//시스템 정기 점검 스케줄러
@Slf4j
@Component
@RequiredArgsConstructor
public class MaintenanceScheduler {
    //실제 정리 로직을 담당하는 유지보수 서비스
    private final MaintenanceService maintenanceService;

    //매일 새벽 4시에 오래된 이벤트,감사 로그 정리 작업 실행
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void purgeOld() {
        int ev = maintenanceService.purgeOldEvents(90);
        int ad = maintenanceService.purgeOldAuditLogs(180);
        log.info("Maintenance purge: event={}, auditLogs={}", ev, ad);
    }
}
