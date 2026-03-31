package com.example.rungame.leaderboard.dto;

import lombok.*;

/*
* 리더보드 한 줄의 정보를 담는 DTO
* - 랭킹 목록 API 응답에 사용
* - 이 유저가 누구고, 어떤 기록을 갖고 있으며, 몇 위인지 한번에 전달
* */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaderboardEntryDTO{

    private Long userId;
    private String nickname;
    private int bestScore;
    private int bestDistance;
    //한 세션에서 모은 최대 코인 수
    private int bestCoins;
    private long rank;
}
