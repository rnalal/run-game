package com.example.rungame.analytics.service;

import com.example.rungame.analytics.repository.CoinAnalyticsRepository;
import com.example.rungame.analytics.repository.EventAnalyticsRepository;
import com.example.rungame.analytics.repository.UserAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * 관리자 분석 서비스
 *
 * - 사용자 성장 지표 분석
 * - 재화(코인) 사용 통계
 * - 이벤트 발생 빈도 분석
 *
 * 분석 전용 Repository(JdbcTemplate 기반)들을 조합해
 * 관리자 대시보드에서 사용할 데이터를 가공·제공
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    //사용자 성장 분석 Repository
    private final UserAnalyticsRepository userAnalyticsRepository;
    //코인 사용량 분석 Repository
    private final CoinAnalyticsRepository coinAnalyticsRepository;
    //이벤트 발생 빈도 분석 Repository
    private final EventAnalyticsRepository eventAnalyticsRepository;

    //==================사용장 성장 분석===================
    /*
    * 사용자 성장 요약 데이터 조회
    *
    * - 주간 신규 가입자 수 추이
    * - 관리자 분석 대시보드 차트용 데이터
    * */
    public Map<String, Object> userGrowthSummary() {
        Map<String, Object> result = new HashMap<>();
        //최근 주간 신규 가입자 통계
        result.put("weeklyNew", userAnalyticsRepository.weeklyUserGrowth());
        return result;
    }

    //===================코인 사용량 분석==================
    /*
    * 코인 사용 통계 조회
    *
    * - 코인 획득량
    * - 코인 사용량
    * - 코인 이벤트가 발생한 세션 수
    * */
    public Map<String, Object> coinStats() {
        return coinAnalyticsRepository.coinUsagesSummary();
    }

    //==================이벤트 사용 통계=========================
    /*
    * 이벤트 발생 빈도 통계 조회
    *
    * - 이벤트 타입별 발생 횟수
    * - 게임 밸런스 및 UX 분석
    * */
    public List<Map<String, Object>> eventFrequency() {
        return eventAnalyticsRepository.topEventFrequency();
    }
}
