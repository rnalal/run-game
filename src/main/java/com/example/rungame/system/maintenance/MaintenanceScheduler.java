package com.example.rungame.system.maintenance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/*
* 시스템 정기 점검 스케줄러
* - 매일 새벽 정해진 시각에 오래된 이벤트 로그와 감사 로그를 정리해서
*   DB 용량을 관리하고 운영 환경의 성능을 꾸준히 유지하는 배치 트리거
* - 실시간 요청 로직에 부담을 주지 않고 새벽 시간에 오래된 로그를 정리함
*
* - 정기 정리 작업 실행
*   - purgeOld()
*       - 매일 새벽 4시에 실행
*       - 90일 지난 게임 이벤트 로그 삭제
*       - 180일 지난 감사 로그 삭제
*       - 실제 삭제 로직은 MaintenanceService에게 위임하고 여기서는 언제,얼마나 지웠는지 로그만 남김
* */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaintenanceScheduler {
    //실제 정리 로직을 담당하는 유지보수 서비스
    private final MaintenanceService maintenanceService;

    /*
    * 매일 새벽 4시에 오래된 이벤트,감사 로그 정리 작업 실행
    *
    * - @Scheduled(cron = "0 0 4 * * *", zone="Asia/Seoul")
    *   - 매일 04:00
    *
    * - 이벤트 로그: 90일 이전 데이터 삭제
    * - 감사 로그: 180일 이전 데이터 삭제
    * - 삭제된 이벤트,감사 로그 건수를 info 레벨로 남겨 운영자가 나중에 정리 결과를 확인할 수 있도록 함
    * */
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void purgeOld() {
        int ev = maintenanceService.purgeOldEvents(90);
        int ad = maintenanceService.purgeOldAuditLogs(180);
        log.info("Maintenance purge: event={}, auditLogs={}", ev, ad);
    }
}
