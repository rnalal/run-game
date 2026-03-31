package com.example.rungame.leaderboard.service;

import com.example.rungame.leaderboard.dto.LeaderboardEntryDTO;
import com.example.rungame.leaderboard.repository.LeaderboardRepository;
import com.example.rungame.user.domain.User;
import com.example.rungame.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
* 리더보드 조회를 담당하는 서비스
* - 유저별 최고 기록을 기준으로 리더보드 목록과 특정 유저의 순위 정보를 만들어주는 도메인 서비스
*
* - 1)기간,정렬 기준에 따라 리더보드 페이지 조회
*       - 전체 기간/최근7일/최근30일
*       - 점수/거리/코인 기준
* - 2)특정 유저의 전체 기간 기준 랭킹 조회
*       - 플레이 기록이 없는 경우 종료된 세션이 없는 경우까지 메시지로 구분
*
* - 컨트롤러는 단순한 파라미터를 받아 이 서비스를 호출
* - 실제 리더보드/순위 로직은 이 서비스 안에서 레포지토리를 조합해 구현
* */
@Service
@RequiredArgsConstructor
public class LeaderboardService {

    /*
    * 리더보드 관련 쿼리를 모아둔 레포지토리
    * - 유저별 최고 기록 조회
    * - 전체/기간별 리더보드
    * - 유저별 순위/통계 계산용 데이터 제공
    * */
    private final LeaderboardRepository leaderboardRepository;

    /*
    * 전체,7일,30일 리더보드 조회
    * - 정렬 기준과 기간에 맞게 레포지토리 메서드를 호출하고
    *   페이지 단위로 랭킹을 붙여서 반환함
    *
    * @param type : 정렬 기준
    * @param range : 기간 범위
    * @param page : 0기반 페이지 번호
    * @param size : 페이지 크기
    * @return : 랭킹 정보다 들어간 리더보드 페이지
    * */
    public Page<LeaderboardEntryDTO> getLeaderboard(String type, String range, int page, int size) {

        //허용되지 않은 type이면 기본값 score로 보정
        if (!List.of("score", "distance", "coins").contains(type)) {
            type = "score";
        }

        PageRequest pageable = PageRequest.of(page, size);

        //최근 7일 리더보드
        if (range.equals("7d")) {
            return applyRanking(
                    leaderboardRepository.findLeaderboardInRange(type, LocalDateTime.now().minusDays(7), pageable),
                    page, size
            );
        }

        //최근 30일 리더보드
        if (range.equals("30d")) {
            return applyRanking(
                    leaderboardRepository.findLeaderboardInRange(type, LocalDateTime.now().minusDays(30), pageable),
                    page, size
            );
        }

        //전체 기간 리더보드
        return applyRanking(
                leaderboardRepository.findLeaderboard(type, pageable),
                page, size
        );
    }

    /*
    * 조회된 페이지에 순위를 채워 넣는 헬퍼 메서드
    * - DB쿼리에서는 rank를 0L로 채워 두고 여기서 실제 페이지/사이즈 기준으로 순위를 설계
    *
    * - startRank = page * size + 1
    * - 페이지 내 i번째 요소의 rank = startRank + i
    * */
    private Page<LeaderboardEntryDTO> applyRanking(Page<LeaderboardEntryDTO> result, int page, int size) {
        long startRank = (long) page * size + 1;

        for (int i = 0; i < result.getContent().size(); i++) {
            result.getContent().get(i).setRank(startRank + i);
        }
        return result;
    }

    /*
    * 특정 유저의 전체 기간 기준 랭킹 조회
    * - 유저의 플레이/종료 세션 여부에 따라 다른 메시지와 함께 반환
    *
    * 반환 형태(Map)
    * - userId : 유저 ID
    * - rank : 유저의 순위 (기록 없으면 null)
    * - bestScore : 해당 유저의 최고 점수 (기록 없으면 null)
    * - mesesage : 상태 설명 (게임 기록 없음, 종료된 세션 없음, 정상 조회)
    * */
    public Map<String, Object> getUserRank(Long userId) {

        //1) 세션이 한 번도 없으면
        if (leaderboardRepository.countAllSessionsByUser(userId) == 0) {
            return Map.of(
                    "userId", userId,
                    "rank", null,
                    "bestScore", null,
                    "message", "게임 기록 없음"
            );
        }

        //2) 세션은 있지만 종료된 세션이 없어 점수를 계산할 수 없는 경우
        Integer bestScore = leaderboardRepository.findUserBestScore(userId);
        if (bestScore == null) {
            return Map.of(
                    "userId", userId,
                    "rank", null,
                    "bestScore", null,
                    "message", "종료된 세션 없음"
            );
        }

        //3) 정상적으로 최고 점수와 순위를 계산할 수 있는 경우
        Long rank = leaderboardRepository.findUserRank(userId);
        if (rank == null) rank = 1L;    //방어적 기본값

        return Map.of(
                "userId", userId,
                "rank", rank,
                "bestScore", bestScore,
                "message", "정상 조회"
        );
    }
}
