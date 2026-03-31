package com.example.rungame.admin.controller;

import com.example.rungame.system.cache.CacheService;
import com.example.rungame.system.maintenance.MaintenanceService;
import com.example.rungame.system.service.SystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/*
* 관리자 시스템 운영 컨트롤러
*
* - 시스템 상태 점검(ping/ health)
* - 캐시 관리 (개별/ 전체 비우기)
* - 유지보수 작업 (데이터 정리)
* - 관리자 백업 가능
*
* 운영 환경 모니터링 및 장애 대응을 위한 관리자 전용 기능을 담당
* */
@Controller
@RequestMapping("/rg-admin/system")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminSystemController {

    //애플리케이션 캐시 관리 서비스
    private final CacheService cacheService;
    //시스템 상태 조회 서비스
    private final SystemService systemService;
    //유지보수 및 데이터 정리 관련 서비스
    private final MaintenanceService maintenanceService;

    //관리자 시스템 관리 페이지(view)
    @GetMapping("/page")
    public String page(){
        return "admin/admin-system";
    }

    /*
    * 관리자 핑(ping) 체크
    *
    * - 관리자 영역 접근 가능 여부 확인
    * - 간단한 상태 확인용 엔드포인트
    * */
    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of(
        "success", true,
        "message", "pong-admin",
        "time", java.time.LocalDateTime.now().toString()
        );
    }

    /*
    * 시스템 헬스 체크
    *
    * - DB, 캐시 등 주요 컴포넌트 상태 조회
    * */
    @GetMapping("/health")
    @ResponseBody
    public Map<String, Object> health() {
        return systemService.health();
    }

    //===========캐시 관리==============
    /*
    * 특정 캐시 비우기
    *
    * - 캐시 이름을 지정하여 선택적으로 제거
    * - ADMIN 이상 권한 필요
    * */
    @PostMapping("/cache/evict")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @ResponseBody
    public Map<String, Object> evictCache(@RequestParam String name) {
        boolean ok = cacheService.evictCache(name);
        return Map.of("success", ok, "cache", name);
    }

    /*
    * 전체 캐시 비우기
    *
    * - 시스템 전체 캐시 초기화
    * - SUPER_ADMIN 전용 기능
    * */
    @PostMapping("/cache.evictAll")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseBody
    public Map<String, Object> evictAll() {
        cacheService.evictAll();
        return Map.of("success", true, "message", "all caches evicted");
    }

    //=============유지보수/ 데이터 정리====================
    /*
    * 오래된 데이터 정리
    *
    * @param eventDays : 이벤트 로그 보관 기간(일)
    * @param auditDays : 감사 로그 보관 기간(일)
    * */
    @PostMapping("/maintenance/purge")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @ResponseBody
    public Map<String, Object> purge(@RequestParam(defaultValue = "90") int eventDays,
                                     @RequestParam(defaultValue = "180") int auditDays) {
        int ev = maintenanceService.purgeOldEvents(eventDays);
        int ad = maintenanceService.purgeOldAuditLogs(auditDays);
        return Map.of("success", true, "eventDeleted", ev, "auditDeleted", ad);
    }

    //====================백업========================
    /*
    * 테이블 CSV 백업
    *
    * - 운영 데이터 수동 백업 용도
    * - SUPER_ADMIN 전용 기능
    * */
    @PostMapping("/backup/csv")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseBody
    public Map<String, Object> backupCsv(@RequestParam String table,
                                         @RequestParam(defaultValue = "/var/backups") String dir) throws Exception {
        //백업 파일 경로 생성
        String out = java.nio.file.Paths.get(dir, table + "-" + System.currentTimeMillis() + ".csv").toString();
        // CSV 파일 생성
        try (var writer = java.nio.file.Files.newBufferedWriter(java.nio.file.Path.of(out))) {
            writer.write("TODO: dump " + table + " here");
        }
        //실제 백업 로직 위임
        String path = maintenanceService.backupTableToCsv(table, out);

        return Map.of("success", true, "path", path);
    }
}
