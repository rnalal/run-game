package com.example.rungame.admin.service;

import com.example.rungame.admin.dto.AdminDashboardEventChartResponse;
import com.example.rungame.admin.dto.AdminDashboardSessionChartResponse;
import com.example.rungame.admin.dto.AdminDashboardSummaryResponse;
import com.example.rungame.event.repository.SessionEventRepository;
import com.example.rungame.session.repository.SessionRepository;
import com.example.rungame.user.repository.UserRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import com.example.rungame.event.domain.EventType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final SessionEventRepository sessionEventRepository;

    public AdminDashboardService(UserRepository userRepository,
                                 SessionRepository sessionRepository,
                                 SessionEventRepository sessionEventRepository){
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.sessionEventRepository = sessionEventRepository;
    }

    //관리자 대시보드 요약 지표 조회
    @Cacheable(value = "admin_dashboard_summary", key = "'summary'")
    public AdminDashboardSummaryResponse summary() {

        System.out.println("🔥 [CACHE MISS] AdminDashboard summary DB 조회 실행");

        //오늘 기준 시각 계산
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();

        //전체 사용자 수
        long userCount = userRepository.count();
        //오늘 시작된 세션 수
        long todaySessionCount = sessionRepository.countByStartedAtAfter(todayStart);
        //오늘 발생한 코인 사용량
        long todayCoinUsage = sessionEventRepository
                .countByTypeAndCreatedAtAfter(EventType.coin_pick, todayStart);

        //일간 활성 사용자
        long dau = sessionRepository.countDistinctUserIdByStartedAtAfter(todayStart);
        //주간 활성 사용자
        long wau = sessionRepository.countDistinctUserIdByStartedAtAfter(todayStart.minusDays(7));
        //월간 활성 사용자
        long mau = sessionRepository.countDistinctUserIdByStartedAtAfter(todayStart.minusDays(30));

        //오늘 신규 가입자 수
        long newUserToday = userRepository.countByCreatedAtAfter(todayStart);
        //최근 7일 신규 가입자 수
        long newUserWeek = userRepository.countByCreatedAtAfter(todayStart.minusDays(7));

        //평균 플레이 시간
        long avgPlaySeconds = sessionRepository.avgPlaySeconds();
        //평균 플레이 시간
        String avgPlayTimeText = formatSeconds(avgPlaySeconds);

        //평균 점수
        long avgScore = sessionRepository.avgScore();

        //충돌 발생 횟수
        long crashCount = sessionEventRepository.countByType(EventType.hit_obstacle);

        //API 에러 수
        long apiErrorCount = 0;

        //JWT 메모리 사용량
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

    //초 단위 시간을 "분 초" 문자열로 변환
    private String formatSeconds(long sec){
        long m = sec / 60;
        long s = sec % 60;
        return m + "분" + s + "초";
    }

    //최근 7일 세션 수 차트 데이터
    @Cacheable(value = "admin_dashboard_session_chart", key = "'session_chart'")
    public AdminDashboardSessionChartResponse getSessionChart() {

        System.out.println("🔥 [CACHE MISS] AdminDashboard session chart DB 조회 실행");

        LocalDate today = LocalDate.now();
        LocalDate from  = today.minusDays(6);
        LocalDate to = today.plusDays(1);

        List<SessionRepository.DailySessionCount> rows =
                sessionRepository.countDailySessions(
                        from.atStartOfDay(),
                        to.atStartOfDay()
                );

        Map<LocalDate, Long> countMap = rows.stream()
                .collect(Collectors.toMap(
                        SessionRepository.DailySessionCount::getDay,
                        row -> row.getSessionCount() == null ? 0L : row.getSessionCount()
                ));

        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();

        for (int i=0; i<7; i++) {
            LocalDate day = from.plusDays(i);

            labels.add(day.getMonthValue() + "/" + day.getDayOfMonth());
            data.add(countMap.getOrDefault(day, 0L));
        }
        return new AdminDashboardSessionChartResponse(labels, data);
    }

    //이벤트 발생 TOP5 차트 데이터
    @Cacheable(value = "admin_dashboard_event_chart", key = "'event_chart'")
    public AdminDashboardEventChartResponse getEventChart() {

        System.out.println("🔥 [CACHE MISS] AdminDashboard event chart DB 조회 실행");

        Map<EventType, Long> map = sessionEventRepository.countGroupByType()
                .stream()
                .collect(Collectors.toMap(
                        SessionEventRepository.EventTypeCount::getType,
                        row -> row.getEventCount() == null ? 0L : row.getEventCount()
                ));

        List<Map.Entry<EventType, Long>> top5 = map.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(5)
                .toList();

        List<String> labels = top5.stream()
                .map(e -> e.getKey().name())
                .toList();

        List<Long> data = top5.stream()
                .map(Map.Entry::getValue)
                .toList();

        return new AdminDashboardEventChartResponse(labels, data);
    }
}
