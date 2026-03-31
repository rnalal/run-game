package com.example.rungame.analytics.service;

import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

/*
 * 서버 모니터링 서비스
 *
 * - 서버 가동 시간
 * - 메모리 사용량
 * - CPU 코어 수
 * - 스레드 개수
 *
 * 관리자 대시보드에서 시스템 상태를
 * 간단히 확인하기 위한 모니터링 기능
 */
@Service
public class MonitoringService {

    /*
    * 서버 성능 및 상태 지표 조회
    *
    * @return
    * - uptime_sec : 서버 가동 시간(초)
    * - memory_used_mb : 현재 사용 중인 메모리(MB)
    * - cpu_cores : CPU 코어 수
    * - thread_count : 현재 스레드 개수
    * */
    public Map<String, Object> serverMetrics() {
        var runtime = Runtime.getRuntime();
        Map<String, Object> metrics = new HashMap<>();
        //서버 가동 시간(초)
        metrics.put("uptime_sec", ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
        //메모리 사용량(MB)
        metrics.put("memory_used_mb", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
        //CPU 코어 수
        metrics.put("cpu_cores", runtime.availableProcessors());
        //현재 활성 스레드 수
        metrics.put("thread_count", ManagementFactory.getThreadMXBean().getThreadCount());
        return metrics;
    }
}
