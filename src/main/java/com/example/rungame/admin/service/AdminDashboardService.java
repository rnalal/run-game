package com.example.rungame.admin.service;

import com.example.rungame.admin.dto.AdminDashboardEventChartResponse;
import com.example.rungame.admin.dto.AdminDashboardSessionChartResponse;
import com.example.rungame.admin.dto.AdminDashboardSummaryResponse;
import com.example.rungame.event.repository.SessionEventRepository;
import com.example.rungame.session.repository.SessionRepository;
import com.example.rungame.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import com.example.rungame.event.domain.EventType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
* 관리자 대시보드 서비스
*
* - 관리자 메인 대시보드에 필요한 핵심 지표 집계
* - 세션/이벤트 통계 차트 데이터 생성
*
* 운영자가 서비스 상태를 한눈에 파악할 수 있도록
* "요약 지표 + 시계열/랭킹 차트" 중심으로 설계
* */
@Service
public class AdminDashboardService {

    //사용자 관련 통계 조회용 Repository
    private final UserRepository userRepository;
    //세션 관련 통계 조회용 Repository
    private final SessionRepository sessionRepository;
    //이베늩 로그 통계 조회용 Repository
    private final SessionEventRepository sessionEventRepository;

    //생성자
    public AdminDashboardService(UserRepository userRepository,
                                 SessionRepository sessionRepository,
                                 SessionEventRepository sessionEventRepository){
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.sessionEventRepository = sessionEventRepository;
    }

    /*
    * 관리자 대시보드 요약 지표 조회
    *
    * - 사용자 / 세션 / 이벤트 / 시스템 상태를 종합한 핵심 수치
    * */
    public AdminDashboardSummaryResponse summary() {

        //오늘 기준 시각 계산
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();

        //===============전체 사용자 수================
        //전체 사용자 수
        long userCount = userRepository.count();
        //오늘 시작된 세션 수
        long todaySessionCount = sessionRepository.countByStartedAtAfter(todayStart);
        //오늘 발생한 코인 사용량 (COIN_PICK 이벤트 기준)
        long todayCoinUsage = sessionEventRepository
                .countByTypeAndCreatedAtAfter(EventType.coin_pick, todayStart);

        //=============활성 사용자 지표==================
        //일간 활성 사용자 (DAU)
        long dau = sessionRepository.countDistinctUserIdByStartedAtAfter(todayStart);
        //주간 활성 사용자 (WAU)
        long wau = sessionRepository.countDistinctUserIdByStartedAtAfter(todayStart.minusDays(7));
        //월간 활성 사용자 (MAU)
        long mau = sessionRepository.countDistinctUserIdByStartedAtAfter(todayStart.minusDays(30));

        //================신규 가입 지표==================
        //오늘 신규 가입자 수
        long newUserToday = userRepository.countByCreatedAtAfter(todayStart);
        //최근 7일 신규 가입자 수
        long newUserWeek = userRepository.countByCreatedAtAfter(todayStart.minusDays(7));

        //==================플레이 지표====================
        //평균 플레이 시간(초)
        long avgPlaySeconds = sessionRepository.avgPlaySeconds();
        //평균 플레이 시간(텍스트 표현)
        String avgPlayTimeText = formatSeconds(avgPlaySeconds);

        //평균 점수
        long avgScore = sessionRepository.avgScore();

        //=====================안정성 / 시스템 지표===============
        //충돌(장애) 발생 횟수
        long crashCount = sessionEventRepository.countByType(EventType.hit_obstacle);

        //API 에러 수
        long apiErrorCount = 0;

        //JWT 메모리 사용량 (MB)
        long memoryMb = (Runtime.getRuntime().totalMemory()
                - Runtime.getRuntime().freeMemory()) / (1024 * 1024);

        //대시보드 요약 DTO 반환
        return new AdminDashboardSummaryResponse(
                userCount,
                todaySessionCount,
                todayCoinUsage,

                dau,
                wau,
                mau,

                newUserToday,
                newUserWeek,

                avgPlayTimeText,
                avgScore,

                crashCount,
                apiErrorCount,
                memoryMb
        );
    }
    /*
    * 초 단위 시간을 "분 초" 문자열로 변환
    *
    * @param sec : 플레이 시간 (초)
    * */
    private String formatSeconds(long sec){
        long m = sec / 60;
        long s = sec % 60;
        return m + "분" + s + "초";
    }

    /*
    * 최근 7일 세션 수 차트 데이터
    *
    * - 관리자 대시보드 일별 세션 그래프 용도
    * */
    public AdminDashboardSessionChartResponse getSessionChart() {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(6); //최근 7일(6일전~오늘)

        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();

        for(int i=0; i<7; i++) {
            LocalDate day = from.plusDays(i);

            //하루 단위 세션 수 집계
            long count = sessionRepository.countByStartedAtBetween(
                    day.atStartOfDay(),
                    day.plusDays(1).atStartOfDay()
            );

            //차트 라벨(MM/DD)
            labels.add(day.getMonthValue() + "/" + day.getDayOfMonth());
            data.add(count);
        }

        return new AdminDashboardSessionChartResponse(labels, data);
    }

    /*
    * 이벤트 발생 TOP5 차트 데이터
    *
    * - 이벤트 타입별 발생 빈도 분석
    * - 관리자 대시보드 이벤트 랭킹 차트 용도
    * */
    public AdminDashboardEventChartResponse getEventChart() {
        Map<EventType, Long> map = new HashMap<>();

        //모든 이벤트 타입별 발생 횟수 집계
        for (EventType type : EventType.values()) {
            long c = sessionEventRepository.countByType(type);
            map.put(type, c);
        }

        //발생 횟수 기준 내림차순 정렬 후 상위 5개 추출
        List<Map.Entry<EventType, Long>> top5 = map.entrySet().stream()
                .sorted((a,b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(5)
                .toList();

        //차트용 라벨 / 데이터 분리
        List<String> labels = top5.stream().map(e -> e.getKey().name()).toList();
        List<Long> data = top5.stream().map(Map.Entry::getValue).toList();

        return new AdminDashboardEventChartResponse(labels, data);
    }
}
