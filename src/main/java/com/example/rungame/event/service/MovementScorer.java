package com.example.rungame.event.service;

import lombok.Getter;
import lombok.Setter;

//현재 이동 상태를 보고 시간 경과에 따라 이동 거리와 점수를 계산해 주는 계산기
public class MovementScorer {

    //튜닝 가능한 상수
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

    //마지막으로 계산한 기준 시각
    private int lastTms;

    //점수 환산용 잔여 px (10px=1점에서 남은 조각 누적)
    // - 누적해서 가지고 있다가 다음 구간 계산 때 함께 반영
    private double carryPxForScoring = 0.0;

    //@param lastTmsAnchor : 이전까지 계산된 마지막 tMs 기준
    //                       - 과거 세션 상태에서 이어서 계산할 때 사용
    public MovementScorer(int lastTmsAnchor){
        this.lastTms = Math.max(0, lastTmsAnchor);
    }

    //외부에서 초기 상태를 한 번에 세팅할 때 사용하는 헬퍼
    public void seedState(boolean reverse, boolean sprint, boolean slide, boolean paused){
        this.reverse = reverse;
        this.sprint = sprint;
        this.slide = slide;
        this.paused = paused;
        this.boost = false;
    }

    //lastTms를 현재 시각으로만 옮기는 함수
    public void moveAnchorTo(int tMs){
        if (tMs > lastTms){
            this.lastTms = tMs;
        }
    }

    //[lastTms, tMs] 구간에 대해 거리/점수 계산
    public Result flushUntil(int tMs, int x2UntilMs){
        if (tMs <= lastTms) return Result.zero();

        double distPx = 0.0;
        int score = 0;

        //구간 분할:
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

    //내부용: 특정 구간을 한 번의 가중치/배수로 계산
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

    //이동/점수 계산 결과를 담는 간단한 DTO 역할 클래스
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
