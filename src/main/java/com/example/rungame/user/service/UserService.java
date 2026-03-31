package com.example.rungame.user.service;

import com.example.rungame.common.jwt.JwtProvider;
import com.example.rungame.session.domain.Session;
import com.example.rungame.session.repository.SessionRepository;
import com.example.rungame.user.domain.User;
import com.example.rungame.user.dto.UserDTO;
import com.example.rungame.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/*
* UserService
* - 회원가입, 로그인, 로그아웃, 내 정보 조회, 비밀번호/닉네임 변경, 회원 탈퇴 등
*   사용자 계정 관련 핵심 비즈니스 로직은 담당함
* - JWT + tokenVersion을 이용해서 로그아웃 이후 토큰 무효화까지 처리함
* - SessionRepository와 연동해서 마이페이지에 보여줄 플레이 통계도 함께 계산
*
* - 비밀번호는 BCrypt로 해시 저장 및 검증
* - JWT 안에 userId, email, nickname, role, tokenVersion 등을 넣고
*   토큰 한 개로 인증 + 권한 + 버전 관리까지 처리
* - 토큰에 들어 있는 tokenVersion과 DB의 tokenVersion을 비교해서
*   로그아웃,비밀번호 변경, 탈퇴 이후의 토큰은 자동으로 막도록 설계
* */
@Service
@RequiredArgsConstructor
public class UserService {

    //유저 DB 접근
    private final UserRepository userRepository;
    //비밀번호 해시,검증
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    //JWT 발급,검증
    private final JwtProvider jwtProvider;
    //플레이 기록,통계 조회
    private final SessionRepository sessionRepository;

    //회원가입
    @Transactional
    public UserDTO.UserRes signUp(UserDTO.SignUpReq req) {
        //1)이메일,닉네임 중복 체크
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        if (userRepository.existsByNickname(req.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }
        //2)비밀번호 해시
        String hash = encoder.encode(req.getPassword());
        //3)User 생성 및 저장
        User saved = userRepository.save(User.create(req.getEmail(), hash, req.getNickname()));
        //4)응답 DTO 매핑
        return UserDTO.UserRes.builder()
                .id(saved.getId())
                .email(saved.getEmail())
                .nickname(saved.getNickname())
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    //로그인(JWT 발급)
    public UserDTO.LoginRes login(UserDTO.LoginReq req){
        //1)이메일로 유저 조회
        User u = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        //2)비밀번호 검증
        if(!encoder.matches(req.getPassword(), u.getPasswordHash())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        //3)계정 상태 체크
        if("BANNED".equalsIgnoreCase(u.getStatus())){
            throw new IllegalArgumentException("접근이 제한된 계정입니다.");
        }
        if("DELETED".equalsIgnoreCase(u.getStatus())) {
            throw new IllegalArgumentException("탈퇴한 계정입니다.");
        }

        //4)JWT 생성(userId, email, nickname, role, tokenVersion 포함)
        String token = jwtProvider.createAccessToken(u.getId(), u.getEmail(), u.getNickname(), u.getRole(), u.getTokenVersion());

        //5)프론트에서 사용할 LoginRes 반환
        long ttl = 3600; //만료까지 남은 시간(초)
        return new UserDTO.LoginRes(token, u.getId(), u.getEmail(), u.getNickname(), ttl);
    }

    //로그아웃 (tokenVersion+1)
    @Transactional
    public UserDTO.LogoutRes logoutByToken(String bearerToken) {
        //1)토큰 서명,만료 검증
        if (!jwtProvider.validate(bearerToken)) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 토큰입니다.");
        }

        //2)토큰에서 userId, tokenVersion 추출
        Long userIdFromToken = jwtProvider.getUserId(bearerToken);
        int tokenVerFromToken = jwtProvider.getVersion(bearerToken);

        //3)DB에서 사용자 조회
        User user = userRepository.findById(userIdFromToken)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        //4)이미 한 번 무효화된 토큰인지 체크
        if (tokenVerFromToken != user.getTokenVersion()) {
            throw new IllegalStateException("이미 무효화된 토큰입니다.");
        }

        //5)tokenVersion 증가 -> 이전에 발급된 토큰 전부 무효
        user.incrementTokenVersion();

        //6)응답
        return UserDTO.LogoutRes.builder()
                .userId(user.getId())
                .newTokenVersion(user.getTokenVersion())
                .message("Logged out")
                .build();
    }

    //내 정보 조회+플레이 통계
    @Transactional
    public UserDTO.MyInfoRes getMyInfoByToken(String bearerToken){
        //1)토큰 검증
        if (!jwtProvider.validate(bearerToken)){
            throw new IllegalArgumentException("유효하지 않거나 만료된 토큰입니다.");
        }

        //2)userId / tokenVersion 추출
        Long userId = jwtProvider.getUserId(bearerToken);
        int ver = jwtProvider.getVersion(bearerToken);

        //3)tokenVersion까지 일치하는 유저만 허용 -> 로그아웃, 탈퇴 토큰 차단
        User user = userRepository.findByIdAndTokenVersion(userId, ver)
                .orElseThrow(() -> new IllegalArgumentException("토큰이 무효화되었거나 사용자 정보를 찾을 수 없습니다."));

        //4)계정 상태 체크
        if(!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new IllegalArgumentException("비활성화된 계정입니다.");
        }

        //5)기본 유저 정보 세팅
        UserDTO.MyInfoRes res = UserDTO.MyInfoRes.from(user);

        //6)플레이 누적,최고,평균 통계 조회
        long sumScore = sessionRepository.sumScoreByUserId(userId);
        long sumDistance = sessionRepository.sumDistanceByUserId(userId);
        long sumCoins = sessionRepository.sumCoinsByUserId(userId);

        //최고 기록
        long bestScore = sessionRepository.bestScoreByUserId(userId);
        long bestDistance = sessionRepository.bestDistanceByUserId(userId);
        long bestCoins = sessionRepository.bestCoinsByUserId(userId);

        //플레이횟수
        long sessionCount = sessionRepository.countByUserId(userId);

        //평균 점수,거리
        double avgScore = sessionCount > 0 ? sessionRepository.avgScoreByUserId(userId) : 0.0;
        double avgDistance = sessionCount > 0 ? sessionRepository.avgDistanceByUserId(userId) : 0.0;

        //최근 플레이 날짜
        LocalDateTime lastPlayed = sessionRepository.lastPlayedAt(userId);

        //DTO에 세팅
        res.setSumScore(sumScore);
        res.setSumDistance(sumDistance);
        res.setSumCoins(sumCoins);

        res.setBestScore(bestScore);
        res.setBestDistance(bestDistance);
        res.setBestCoins(bestCoins);

        res.setSessionCount(sessionCount);
        res.setAvgScore(avgScore);
        res.setAvgDistance(avgDistance);
        res.setLastPlayedAt(lastPlayed != null ? lastPlayed.toString() : null);

        //최근 플레이 5개 요약
        List<Session> sessions = sessionRepository.findTop5ByUserIdOrderByStartedAtDesc(userId);

        List<UserDTO.MyInfoRes.SessionSummary> recent = sessions.stream()
                .map(s -> UserDTO.MyInfoRes.SessionSummary.builder()
                        .id(s.getId())
                        .startedAt(s.getStartedAt() != null ? s.getStartedAt().toString() : null)
                        .endedAt(s.getEndedAt() != null ? s.getEndedAt().toString() : null)
                        .score(s.getScore())
                        .distance(s.getDistance())
                        .coins(s.getCoins())
                        .build())
                .toList();

        res.setRecentSessions(recent);

        return res;
    }

    //비밀번호 변경
    @Transactional
    public void changePasswordByToken(String bearerToken, UserDTO.ChangePasswordReq req) {
        if (!jwtProvider.validate(bearerToken)) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 토큰입니다.");
        }

        Long userId = jwtProvider.getUserId(bearerToken);
        int ver = jwtProvider.getVersion(bearerToken);

        User user = userRepository.findByIdAndTokenVersion(userId, ver)
                .orElseThrow(() -> new IllegalArgumentException("토큰이 무효화되었거나 사용자 정보를 찾을 수 없습니다."));

        //현재 비밀번호 확인
        if (!encoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }

        //새 비밀번호로 변경 + 토큰 버전 증가(기존 토큰 무효화)
        String newHash = encoder.encode(req.getNewPassword());
        user.setPasswordHash(newHash);
        user.incrementTokenVersion();
    }

    //닉네임 변경
    @Transactional
    public void changeNicknameByToken(String bearerToken, UserDTO.ChangeNicknameReq req) {
        if(!jwtProvider.validate(bearerToken)) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 토큰입니다.");
        }

        Long userId = jwtProvider.getUserId(bearerToken);
        int ver = jwtProvider.getVersion(bearerToken);

        User user = userRepository.findByIdAndTokenVersion(userId, ver)
                .orElseThrow(() -> new IllegalArgumentException("토큰이 무효화되었거나 사용자 정보를 찾을 수 없습니다."));

        //닉네임 중복 체크
        if (userRepository.existsByNickname(req.getNewNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        user.setNickname(req.getNewNickname());
    }

    //회원탈퇴
    @Transactional
    public void deleteMyAccount(String bearerToken) {
        if(!jwtProvider.validate(bearerToken)) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 토큰입니다.");
        }

        Long userId = jwtProvider.getUserId(bearerToken);
        int ver = jwtProvider.getVersion(bearerToken);

        User user = userRepository.findByIdAndTokenVersion(userId, ver)
                .orElseThrow(() -> new IllegalArgumentException("토큰이 무효화 되었거나 사용자 정보를 찾을 수 없습니다."));

        //이미 탈퇴된 계정 방지
        if("DELETED".equalsIgnoreCase(user.getStatus())) {
            throw new IllegalArgumentException("이미 탈퇴한 계정입니다.");
        }

        //상태 DELETED 처리 + 닉네임 더 이상 사용 못하게 변경 + 토큰 버전 증가
        user.setStatus("DELETED");
        user.setNickname(user.getNickname() + "_deleted_" + System.currentTimeMillis());
        user.incrementTokenVersion();
    }
}
