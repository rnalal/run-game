package com.example.rungame.leaderboard.controller;

import com.example.rungame.leaderboard.dto.LeaderboardEntryDTO;
import com.example.rungame.leaderboard.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/*
* 리더보드 조회 전용 REST 컨트롤러
* - 전체/7일/30일 기준 상위 랭킹 목록 조회
* - 특정 유저의 현재 랭킹 정보 조회
*
* - 컨트롤러는 파라미터 파싱 + 응답 포맷만 담당
* - 실제 랭킹 계산/조회 로직은 LeaderboardService로 위임
* */
@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    /*
    * 리더보드 조회 로직을 담당하는 서비스
    * - 정렬 기준, 기간, 페이징에 따라 적절한 랭킹 데이터를 만들어 반환
    * */
    private final LeaderboardService leaderboardService;

    /*
    * 리더보드 조회
    * - type: 정렬 기준
    * - range: 집계 기간
    * - page: 0 기반 페이지 번호
    * - size: 페이지 당 항목 수
    *
    * @return : Page<LeaderboardEntryDTO>
      -> 한 페이지 분량의 리더보드 엔트리와 페이징 메타데이터를 함께 전달
    * */
    @GetMapping
    public Page<LeaderboardEntryDTO> getLeaderboard(
            @RequestParam(defaultValue = "score") String type,
            @RequestParam(defaultValue = "all") String range,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return leaderboardService.getLeaderboard(type, range, page, size);
    }

    /*
    * 특정 유저의 현재 랭킹 조회
    * - client 쪽에서 내가 지금 전체/기간별 몇 위인지를 보여줄 때 사용
    * - userId는 인증 정보에서 가져오도록 변경할 여지는 있지만
    *   현재 구조에서는 쿼리 파라미터로 전달받음
    *
    * @return: 서비스에서 계산한 랭킹 정보를 그대로 감싼 ResponseEntity
    * */
    @GetMapping("/rank")
    public ResponseEntity<?> getUserRank(@RequestParam Long userId) {
        return ResponseEntity.ok(leaderboardService.getUserRank(userId));
    }

}
