package com.example.rungame.leaderboard.dto;

import lombok.*;

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
