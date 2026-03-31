package com.example.rungame.leaderboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/*
* 리더보드 전체 상태를 한 번에 요약해서 보여줄 때 쓰는 응답 DTO
* - 이번 구간에서 몇 명이 참여했고, 평균 점수/최고 점수/상위 컷이 어느 정도인지
*   점수 분포는 어떻게 생겼는지를 한 번에 내려주는 통계 요약용 모델
* */
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
    /*
    * 점수 분포 히스토그램
    * - key: 구간 이름
    * - value: 해당 구간에 속한 유저 수
    * */
    private Map<String, Long> histogram;
}
