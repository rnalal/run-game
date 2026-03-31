package com.example.rungame.event.service;

import lombok.Getter;
import lombok.Setter;

/*
* 현재 이동 상태를 보고 시간 경과에 따라 이동 거리와 점수를 계산해 주는 계산기
*
* 기본 규칙
* - 기본 속도: BASE_SPEED_PX_PER_MS
* - 10px = 1점
* - reverse=true 인 동안은 점수 0, 거리만 증가
* - sprint/slide/paused/boost 상태에 따라 속도에 가중치 적용
* - SCORE_X2 활성 구간과 겹치는 경우는 해당 구간만 점수 2배로 계산
* */
public class MovementScorer {
    //튜닝 가능한 상수
    //100 px/s
    private static final double BASE_SPEED_PX_PER_MS = 0.10;
    private static final double SPRINT_MUL = 1.5;
    private static final double SLIDE_MUL = 1.1;
    private static final double PAUSE_MUL = 0.0;
    //SPEED_BOOST 시 거리 가중치
    private static final double BOOST_MUL = 1.8;

    //현재 상태 이동 플래그
    @Getter @Setter
    private boolean reverse;    //true만 점수 0, 거리만 증가
    @Getter @Setter
    private boolean sprint;
    @Getter @Setter
    private boolean slide;
    @Getter @Setter
    private boolean paused;
    @Getter @Setter
    private boolean boost;

    //마지막으로 계산한 기준 시각(ms 단위, nowMs 기준)
    private int lastTms;

    //점수 환산용 잔여 px (10px=1점에서 남은 조각 누적)
    // - 누적해서 가지고 있다가 다음 구간 계산 때 함께 반영
    private double carryPxForScoring = 0.0;

    //@param lastTmsAnchor : 이전까지 계산된 마지막 tMs 기준
    //                       - 과거 세션 상태에서 이어서 계산할 때 사용
    public MovementScorer(int lastTmsAnchor){
        this.lastTms = Math.max(0, lastTmsAnchor);
    }

    /*
    * 외부에서 초기 상태를 한 번에 세팅할 때 사용하는 헬퍼
    * - reverse/sprint/slide/paused 상태를 한 번에 맞춰 넣어주는 용도
    * - boost는 여기서 false로 초기화
    * */
    public void seedState(boolean reverse, boolean sprint, boolean slide, boolean paused){
        this.reverse = reverse;
        this.sprint = sprint;
        this.slide = slide;
        this.paused = paused;
        this.boost = false;
    }

    /*
    * lastTms를 현재 시각으로만 옮기는 함수
    * - 아직 이동 계산을 하지 않고 다음 flush의 기준 시각만 옮기고 싶을 때 사용
    * - tMs가 더 과거면 무시
    * */
    public void moveAnchorTo(int tMs){
        if (tMs > lastTms){
            this.lastTms = tMs;
        }
    }

    /*
        [lastTms, tMs] 구간에 대해 거리/점수 계산
        - SCORE_X2의 종료 시각과 구간이 겹치는 경우 -> 필요하다면 구간을 두 부분으로 나누어 각각 따로 계산

        @param tMs : 이번 이벤트 시각
        @param x2UntilMs : SCORE_X2 종료 시각
        @return distPx(절대 이동 거리, floor 후 점수), scoreDelta(추가 점수)
    */
    public Result flushUntil(int tMs, int x2UntilMs){
        if (tMs <= lastTms) return Result.zero();

        double distPx = 0.0;
        int score = 0;

        /*
        * 구간 분할:
        * lastTms ~ tMs 사이에 x2UntilMs 경계가 껴 있으면
        * [lastTms, x2UntilMs], [x2UntilMs, tMs] 두 구간으로 나눠 계산
        * */
        if (x2UntilMs > lastTms && x2UntilMs < tMs){
            Result r1 = computeSegment(lastTms, x2UntilMs, true);
            Result r2 = computeSegment(x2UntilMs, tMs, false);
            distPx += r1.distPx + r2.distPx;
            score += r1.scoreDelta + r2.scoreDelta;
        } else {
            boolean x2Active = (tMs <= x2UntilMs);
            Result r = computeSegment(lastTms, tMs, x2Active);
            distPx += r.distPx;
            score += r.scoreDelta;
        }

        this.lastTms = tMs;
        return new Result((int)Math.floor(distPx), score);
    }

    /*
    * 내부용: 특정 구간을 한 번의 가중치/배수로 계산
    * - sprint/slide/pause/boost 상태에 따라 속도 배수를 적용한 뒤
    *   px와 점수를 계산
    * - x2Active = true 라면 계산된 점수에 x2를 적용
    * */
    private Result computeSegment(int from, int to, boolean x2Active){
        int dt = Math.max(0, to - from);
        if (dt == 0) return Result.zero();

        //속도 가중치 계산
        double mul = paused ? PAUSE_MUL : 1.0;
        if (!paused) {
            if (sprint) mul *= SPRINT_MUL;
            if (slide) mul *= SLIDE_MUL;
            if (boost) mul *= BOOST_MUL; //SPEED_BOOST 활성 시 추가 가중치
        }

        //기본 속도 * 시간 * 가중치 = 이동 거리
        double px = dt * BASE_SPEED_PX_PER_MS * mul;

        //점수 환산 (10px = 1점) + 잔여 누적
        double totalPxForScore = carryPxForScoring + px;
        int baseScore = reverse ? 0 : (int)Math.floor(totalPxForScore / 10.0);
        int score = x2Active ? (baseScore * 2) : baseScore;

        //남은 px를 carry로 유지해 다음 구간 계산에 반영
        carryPxForScoring = totalPxForScore - (baseScore * 10.0);

        return new Result(px, score);
    }

    /*
    * 이동/점수 계산 결과를 담는 간단한 DTO 역할 클래스
    * - distPx : 이동 거리
    * - scoreDelta : 이번 구간에서 추가된 점수
    * */
    public static class Result {
        public final double distPx; //소수 포함(내부 합산용)
        public final int scoreDelta; //정수 점수

        public Result(double distPx, int scoreDelta){
            this.distPx = distPx;
            this.scoreDelta = scoreDelta;
        }

        public static Result zero(){ return new Result(0.0, 0); }
    }
}
