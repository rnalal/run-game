package com.example.rungame.leaderboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardSummaryResponse {

    //"7d", "30d", "season", "all"
    private String range;
    private long totalUsers;
    //유저별 최고 점수의 평균
    private double avgBestScore;
    //전체 최고 점수
    private int maxScore;
    //상위 1% 컷 점수
    private int top1PercentScore;
    //점수 분포 히스토그램
    private Map<String, Long> histogram;
}
