package com.example.rungame.session.service;

import com.example.rungame.event.domain.EventType;
import com.example.rungame.event.repository.SessionEventRepository;
import com.example.rungame.session.domain.Session;
import com.example.rungame.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/*
* 세션 모니터링 전용 서비스
* - 현재 서비스에서 게임이 얼마나 활발하게 돌고 있는지를 간단한 숫자 지표로 묶어서
*   제공하는 모니터링용 서비스
*
* 제공하는 정보
* - 활성 세션 수
* - 활성 세션의 평균 진행 시간
* - 최근 N분 동안 발생한 장애물 충돌 이벤트 개수
*
* - 비즈니스 로직을 한 곳에 모아서 컨트롤러에서는 Map 결과만 받아서 바로 JSON 응답으로
*   내려줄 수 있게함
* */
@Service
@RequiredArgsConstructor
public class SessionMonitorService {

    //세션 상태,통계 관련 집계를 담당하는 레포지토리
    private final SessionRepository sessionRepository;

    //이벤트 타입별 발생 건수를 집계하는 레포지토리
    private final SessionEventRepository sessionEventRepository;

    /*
    * 최근 N분 기준 간단 모니터링 값 조회
    *
    * @param minutes : 기준이 되는 분 단위
    * @return : 아래 키를 가진 Map
    *   - activeSessions : 현재 ACTIVE 상태인 세션 수
    *   - avgActiveSeconds : ACTIVE 세션들의 평균 진행 시간
    *   - recentHitCount : 최근 N분 동안 발생한 hit_obstacle 이벤트 수
    *   - windowMinutes : 집계 기준이 된 분 값
    *
    * - 1) 현재 진행 중인 세션 수 집계
    * - 2) 진행 중인 세션들의 평균 진행 시간 계산
    * - 3) now-minutes 이후에 생성된 hit_obstacle 이벤트 개수 집계
    * - 4) 위 값들을 Map으로 묶어서 반환
    * */
    public Map<String, Object> snapshot(int minutes){
        //1)활성 세션 수
        long activeCount = sessionRepository.countByStatus(Session.Status.ACTIVE);

        //2)활성 세션 평균 진행 시간
        long avgActiveSec = sessionRepository.avgActiveSecondsForActiveSessions();

        //3)최근 N분 동안의 장애물 충돌 이벤트 수
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        long recentHits = sessionEventRepository.countByTypeAndCreatedAtAfter(EventType.hit_obstacle, since);

        //4)대시보드에서 바로 쓰기 좋은 형태로 묶어서 반환
        return Map.of(
                "activeSessions", activeCount,
                "avgActiveSeconds", avgActiveSec,
                "recentHitCount", recentHits,
                "windowMinutes", minutes
        );
    }
}
