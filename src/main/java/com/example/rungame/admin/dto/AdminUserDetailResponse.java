package com.example.rungame.admin.dto;

import com.example.rungame.session.domain.Session;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

/*
* 관리자 사용자 상세 정보 응답 DTO
*
* - 사용자 기본 정보
* - 누적 플레이 통계
* - 최근 세션/ 이벤트 활동 내역
*
* 관리자 화면에서 사용자 한 명을 깊이 분석하기 위한 종합 데이터 구조
* */
@Value
@Builder
public class AdminUserDetailResponse {
    //============기본 사용자 정보=============
    Long id;
    String nickname;
    String email;
    String role;
    String status;
    LocalDateTime createdAt;
    LocalDateTime lastLoginAt;

    //============전체 활동 요약============
    //전체 세션 수
    long totalSessions;

    //============누적 집계 데이터===========
    //누적 점수 합계
    long totalScore;
    //누적 이동 거리
    long totalDistance;
    //누적 획득 코인 수
    long totalCoins;
    //누적 플레이 시간 (초 단위)
    long totalPlaySeconds;

    //=============최근 활동 내역=============
    //최근 세션 목록
    List<RecentSession> recentSessions;
    //최근 이벤트 목록
    List<RecentEvent> recentEvents;

    //=============최근 세션 정보============
    //최근 세션 요약 DTO
    @Value
    public static class RecentSession {
        Long sessionId;
        int score;
        int distance;
        int coins;
        LocalDateTime startedAt;
        LocalDateTime endedAt;

        //Session 엔티티 -> RecentSession 변환
        public static RecentSession from(Session s) {
            return new RecentSession(
                    s.getId(), s.getScore(), s.getDistance(), s.getCoins(),
                    s.getStartedAt(), s.getEndedAt()
            );
        }
    }

    //===============최근 이벤트 정보==============
    /*
    * 최근 이벤트 요약 DTO
    *
    * - payload가 큰 경우 일부만 미리보기로 제공
    * */
    @Value
    public static class RecentEvent {
        Long eventId;
        Long sessionId;
        String type;
        LocalDateTime createdAt;
        String payloadPreview; //payload가 크면 앞부분 미리보기

        //SessionEvent 엔티티 -> RecentEvent 변환
        public static RecentEvent from(com.example.rungame.event.domain.SessionEvent e){
            String preview = e.getPayload();
            //payload 길이가 길 경우 앞부분만 잘라서 제공
            if (preview != null && preview.length() > 120) {
                preview = preview.substring(0, 120) + "...";
            }
            return new RecentEvent(e.getId(), e.getSessionId(), e.getType().name(), e.getCreatedAt(), preview);

        }
    }
}
