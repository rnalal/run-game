package com.example.rungame.analytics.service;

import com.example.rungame.analytics.repository.CoinAnalyticsRepository;
import com.example.rungame.analytics.repository.EventAnalyticsRepository;
import com.example.rungame.analytics.repository.UserAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final UserAnalyticsRepository userAnalyticsRepository;
    private final CoinAnalyticsRepository coinAnalyticsRepository;
    private final EventAnalyticsRepository eventAnalyticsRepository;

    //사용자 성장 요약 데이터 조회
    public Map<String, Object> userGrowthSummary() {
        Map<String, Object> result = new HashMap<>();
        //최근 주간 신규 가입자 통계
        result.put("weeklyNew", userAnalyticsRepository.weeklyUserGrowth());
        return result;
    }

    //코인 사용 통계 조회
    public Map<String, Object> coinStats() {
        return coinAnalyticsRepository.coinUsagesSummary();
    }

    //이벤트 발생 빈도 통계 조회
    public List<Map<String, Object>> eventFrequency() {
        return eventAnalyticsRepository.topEventFrequency();
    }
}
