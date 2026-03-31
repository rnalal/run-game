package com.example.rungame.session.service;

import com.example.rungame.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/*
* 세션 통계 조회 서비스
* - 게임이 실제로 얼마나 플레이되고 있는지 날짜 구간별로 몇 판이나 플레이됐고, 평균 점수는 어느 정도인지 같은
*   일자 단위 운영 지표를 조회하는 서비스
*
* - 일자 기준 집계 조회
*   - dailyStatus(from, to)
*       - ended_at을 기준으로 [from~to] 구간에 대해 날짜별 세션 수, 평균 점수/코인/거리 등을 한 번에 가져옴
*       - DB에 정의된 native 쿼리를 그대로 감싸서 서비스 계층에서 재사용하기 쉽게 제공하는 역할
* */
@Service
@RequiredArgsConstructor
public class SessionStatsService {

    private final SessionRepository sessionRepository;

    /*
    * [from~to] 구간의 일별 세션 통계 조회
    *
    * @param from : 포함 시작일
    * @param to : 포함 종료일
    * @return : 각 원소는 한 날짜에 대한 통계 Map
    *   - day : LocalDate 형태의 날짜
    *   - session_cnt : 그날 종료된 세션 수
    *   - avg_score : 평균 점수
    *   - avg_coins : 평균 코인
    *   - avg_distance : 평균 거리
    * */
    public List<Map<String, Object>> dailyStats(LocalDate from, LocalDate to) {
        //ended_at 기준 [from, to] 일자 구간 집계
        return sessionRepository.aggregateDailyStats(from, to);
    }

}
