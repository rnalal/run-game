package com.example.rungame.admin.controller;

import com.example.rungame.system.cache.CacheService;
import com.example.rungame.system.maintenance.MaintenanceService;
import com.example.rungame.system.service.SystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/rg-admin/system")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminSystemController {

    private final CacheService cacheService;
    private final SystemService systemService;
    private final MaintenanceService maintenanceService;

    @GetMapping("/page")
    public String page(){
        return "admin/admin-system";
    }

    //관리자 ping 체크
    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of(
        "success", true,
        "message", "pong-admin",
        "time", java.time.LocalDateTime.now().toString()
        );
    }

    //시스템 헬스 체크
    @GetMapping("/health")
    @ResponseBody
    public Map<String, Object> health() {
        return systemService.health();
    }

    //특정 캐시 비우기
    @PostMapping("/cache/evict")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @ResponseBody
    public Map<String, Object> evictCache(@RequestParam String name) {
        boolean ok = cacheService.evictCache(name);
        return Map.of("success", ok, "cache", name);
    }

    //전체 캐시 비우기
    @PostMapping("/cache.evictAll")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseBody
    public Map<String, Object> evictAll() {
        cacheService.evictAll();
        return Map.of("success", true, "message", "all caches evicted");
    }

    //오래된 데이터 정리
    @PostMapping("/maintenance/purge")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @ResponseBody
    public Map<String, Object> purge(@RequestParam(defaultValue = "90") int eventDays,
                                     @RequestParam(defaultValue = "180") int auditDays) {
        int ev = maintenanceService.purgeOldEvents(eventDays);
        int ad = maintenanceService.purgeOldAuditLogs(auditDays);
        return Map.of("success", true, "eventDeleted", ev, "auditDeleted", ad);
    }

    //테이블 CSV 백업
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
