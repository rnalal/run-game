package com.example.rungame.admin.service;

import com.example.rungame.admin.dto.AdminUserActivityResponse;
import com.example.rungame.admin.dto.AdminUserDetailResponse;
import com.example.rungame.admin.dto.AdminUserResponse;
import com.example.rungame.admin.spec.UserAdminSpecs;
import com.example.rungame.event.repository.SessionEventRepository;
import com.example.rungame.session.repository.SessionRepository;
import com.example.rungame.user.domain.User;
import com.example.rungame.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/*
* 관리자 사용자 관리 서비스
*
* - 사용자 검색 (조건 + 페이징)
* - 사용자 상세 정보 조회
* - 사용자 정지 / 정지 해제
* - 사용자 권한 변경
* - 사용자 활동 요약 조회
*
* 운영 및 관리 목적의 사용자 제어 로직을 담당
* */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    //사용자 기본 정보 Repository
    private final UserRepository userRepository;
    //세션 집계 및 조회 Repository
    private final SessionRepository sessionRepository;
    //사용자 이벤트 조회 Repository
    private final SessionEventRepository sessionEventRepository;

    //===============사용자 검색 (조건+페이징)=======================
    /*
    * 사용자 목록 검색
    *
    * - ID / 닉네임 / 이메일 / 상태 / 권한 조건 지원
    * - JPA Specification 기반 동적 쿼리
    * */
    public Page<AdminUserResponse> searchUsers(Long id, String nickname, String email, String status,String role, Pageable pageable){
        Specification<User> spec = Specification.where(UserAdminSpecs.idEquals(id))
                .and(UserAdminSpecs.nicknameContains(nickname))
                .and(UserAdminSpecs.emailContains(email))
                .and(UserAdminSpecs.statusEquals(status))
                .and(UserAdminSpecs.roleEquals(role));

        return userRepository.findAll(spec, pageable)
                .map(AdminUserResponse::from);
    }

    //=========================사용자 상세 정보 조회=======================
    /*
    * 사용자 상세 정보 조회
    *
    * - 기본 프로필
    * - 누적 플레이 통계
    * - 최근 세션 / 이벤트
    * */
    public AdminUserDetailResponse getUserDetail(Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        //최근 세션 5개
        var recent = sessionRepository.findTop5ByUserIdOrderByStartedAtDesc(u.getId())
                .stream()
                .map(AdminUserDetailResponse.RecentSession::from)
                .collect(Collectors.toList());
        //전체 세션 수
        long total = sessionRepository.countByUserId(u.getId());

        //누적 집계 데이터
        long totalScore = sessionRepository.sumScoreByUserId(u.getId());
        long totalDistance = sessionRepository.sumDistanceByUserId(u.getId());
        long totalCoins = sessionRepository.sumCoinsByUserId(u.getId());
        long totalPlaySeconds = sessionRepository.sumPlaySecondsByUserId(u.getId());

        //최근 이벤트 50개
        var recentEvents = sessionEventRepository.findTop50ByUserIdOrderByCreatedAtDesc(u.getId())
                .stream()
                .map(AdminUserDetailResponse.RecentEvent::from)
                .collect(Collectors.toList());

        return AdminUserDetailResponse.builder()
                .id(u.getId())
                .nickname(u.getNickname())
                .email(u.getEmail())
                .role(u.getRole())
                .status(u.getStatus())
                .createdAt(u.getCreatedAt())
                .lastLoginAt(u.getLastLoginAt())
                .totalSessions(total)
                .totalScore(totalScore)
                .totalDistance(totalDistance)
                .totalCoins(totalCoins)
                .totalPlaySeconds(totalPlaySeconds)
                .recentSessions(recent)
                .recentEvents(recentEvents)
                .build();
    }

    //====================사용자 정지 / 정지 해제======================
    /*
    * 사용자 정지 / 정지 해제
    *
    * - 상태 변경
    * - 토큰 버전 증가를 통한 즉시 로그아웃 효과
    * */
    @Transactional
    public void updateBan(Long userId, boolean ban){
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        //상태 변경
        u.setStatus(ban ? "BANNED" : "ACTIVE");

        //기본 JWT 무효화를 위한 토큰 버전 증가
        u.incrementTokenVersion();
    }

    //=====================사용자 권한 변경=========================
    /*
    * 사용자 권한 변경
    *
    * - USER / ADMIN / SUPER_ADMIN
    * */
    @Transactional
    public void updateRole(Long userId, String newRole) {

        if (newRole == null) throw new IllegalArgumentException("Role is required");
        String nr = newRole.trim().toUpperCase();

        if(!nr.equals("USER") && !nr.equals("ADMIN") && !nr.equals("SUPER_ADMIN")) {
            throw new IllegalArgumentException("Invalid role: " + newRole);
        }

        User u = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        u.setRole(nr);
    }

    //=====================사용자 활동 요약==========================
    /*
    * 사용자 활동 요약 조회
    *
    * - 최근 로그인 시각
    * - 최근 N일 세션 수
    * - 전체 누적 플레이 시간
    * */
    @Transactional(readOnly = true)
    public AdminUserActivityResponse getUserActivity(Long userId, int days) {
        var u = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime from = now.minusDays(days <= 0 ? 30 : days);

        //기간 내 세션 수
        long sessionCountInRange = sessionRepository.countByUserIdAndStartedAtBetween(userId, from, now);
        //전체 누적 플레이 시간
        long playSecondsTotal = sessionRepository.sumPlaySecondsByUserId(userId);

        /*
        * 기간 내 플레이 시간
        *
        * - 진해 중 세션 (endedAt == null)은 제외
        * - 필요 시 조건 보강 가능
        * */
        long playSecondsInRange = 0L;

        return new com.example.rungame.admin.dto.AdminUserActivityResponse(
                u.getId(),
                u.getLastLoginAt(),
                sessionCountInRange,
                playSecondsTotal,
                playSecondsInRange
        );
    }
}
