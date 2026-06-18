package com.example.rungame.analytics.service;

import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

@Service
public class MonitoringService {

    //서버 성능 및 상태 지표 조회
    public Map<String, Object> serverMetrics() {

        var runtime = Runtime.getRuntime();

        Map<String, Object> metrics = new HashMap<>();

        //서버 가동 시간
        metrics.put("uptime_sec", ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
        //메모리 사용량
        metrics.put("memory_used_mb", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
        //CPU 코어 수
        metrics.put("cpu_cores", runtime.availableProcessors());
        //현재 활성 스레드 수
        metrics.put("thread_count", ManagementFactory.getThreadMXBean().getThreadCount());
        return metrics;
    }
}
