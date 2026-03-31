// draw() 메인
// draw.js

import { gimmicks } from "/js/gimmicks.js";

import { S } from "../state.js";
import { ctx, W, H } from "../dom.js";
import { nowMs } from "../utils.js";
import { drawBaseBackground, drawVignetteIfNeeded } from "./background.js";
import { drawCheckpoints, drawObstacles, drawCoins, drawWindBoosts } from "./objects.js";
import { drawPlayer } from "./player.js";
import { isShieldActive, isSpeedBoostActive } from "../powerup.js";

/*
화면 전체 렌더링 모듈
- 배경,오브젝트,버프 이펙트,플레이어를 한 번에 그리는 메인 드로우 루프
*/

//바람(스프린트,바람 부스트) 아이콘
export function drawWindIcon(x, y, r, t) {
  ctx.save();
  ctx.translate(x, y + Math.sin(t/400)*4);

  //바깥 원(글로우 느낌)
  ctx.beginPath();
  ctx.arc(0, 0, r+6, 0, Math.PI*2);
  ctx.globalAlpha = 0.18;
  ctx.fillStyle = "#38bdf8";
  ctx.fill();
  ctx.globalAlpha = 1;

  //바람 곡선 라인들
  ctx.strokeStyle = "#0ea5e9";
  ctx.lineWidth = 2.2;
  ctx.lineCap = "round";

  ctx.beginPath();
  ctx.moveTo(-r, -r/3);
  ctx.quadraticCurveTo(-r/4, -r, r/2, -r/4);
  ctx.stroke();

  ctx.beginPath();
  ctx.moveTo(-r+2, 0);
  ctx.quadraticCurveTo(r/8, -r/4, r-2, 0);
  ctx.stroke();

  ctx.beginPath();
  ctx.moveTo(-r/2, r/3);
  ctx.quadraticCurveTo(r/4, r/2, r/2, r/3);
  ctx.stroke();

  //작은 포인트 하이라이트
  ctx.fillStyle = "#e0f2fe";
  ctx.beginPath();
  ctx.arc(r/2, -r/3, 2.4, 0, Math.PI*2);
  ctx.fill();

  ctx.restore();
}

//한 프레임 렌더링 (메인 드로우 루프)
export function draw(){
  const w = W(), h = H();
  const isAir = !S.player.onGround; //점프 중인지 여부

  //화면 흔들림 적용
  ctx.save();
  let shakeX = 0, shakeY = 0;
  if (S.shakeTime > 0) {
    const tNorm = Math.max(0, S.shakeTime) / 400;
    const strength = S.shakeIntensity * tNorm;
    shakeX = (Math.random() - 0.5) * strength;
    shakeY = (Math.random() - 0.5) * strength;
    S.shakeTime -= S.lastDeltaMs;
  }
  ctx.translate(shakeX, shakeY);

  //흔들림 보정 포함해서 전체 지우기
  ctx.clearRect(-shakeX, -shakeY, w, h);

  const t = nowMs();

  //배경(하늘,언덕,도로)
  drawBaseBackground(t);

  //체크포인트/장애물/코인/바람 부스트
  drawCheckpoints();
  drawObstacles(t);
  drawCoins();
  drawWindBoosts(t);

  //SCORE_X2 오라
  const x2remain = S.scoreX2UntilMsClient - t;
  if (x2remain > 0) {
    ctx.beginPath();
    ctx.arc(
      S.player.x + S.player.w / 2,
      S.player.y + S.player.h / 2,
      26 + (isAir ? 0 : Math.sin(t / 120) * 2), //점프 중 진동 제거
      0,
      Math.PI * 2
    );
    ctx.strokeStyle = "#22c55e";
    ctx.lineWidth = 3;
    ctx.globalAlpha = 0.9;
    ctx.stroke();
    ctx.globalAlpha = 1;
  }

  //SPEED_BOOST 오라 + 속도 라인
  const boostRemain = S.speedBoostUntilMs - t;
  if (boostRemain > 0) {
    const cxp = S.player.x + S.player.w / 2;
    const cyp = S.player.y + S.player.h / 2;
    const pulse = isAir ? 3 : 3 + Math.sin(t / 100) * 2; //점프 중에는 고정

    //바깥쪾 오라 링
    ctx.beginPath();
    ctx.arc(cxp, cyp, 30 + pulse, 0, Math.PI * 2);
    ctx.strokeStyle = "#f97316";
    ctx.lineWidth = 3;
    ctx.globalAlpha = 0.75;
    ctx.stroke();
    ctx.globalAlpha = 1;

    //뒤로 날리는 속도 라인
    ctx.strokeStyle = "rgba(249,115,22,0.8)";
    ctx.lineWidth = 2;
    for (let i = 0; i < 4; i++) {
      const lx = S.player.x - 10 - i * 6;
      const ly = S.player.y + 8 + i * 4;
      ctx.beginPath();
      ctx.moveTo(lx, ly);
      ctx.lineTo(lx - 14, ly + 3);
      ctx.stroke();
    }
  }

  //SHIELD 보호막(육각형 방패 + 안쪽 글로우)
  if (isShieldActive()) {
    const cxp = S.player.x + S.player.w / 2;
    const cyp = S.player.y + S.player.h / 2;
    const R = isAir ? 32 : 32 + Math.sin(t / 120) * 1.5; //점프 중 진동 제거
    const sides = 6;
    const rot = (t / 600) % (Math.PI * 2);

    //육각형 외곽선
    ctx.beginPath();
    for (let i = 0; i < sides; i++) {
      const a = rot + i * (Math.PI * 2 / sides);
      const xx = cxp + R * Math.cos(a);
      const yy = cyp + R * Math.sin(a);
      if (i === 0) ctx.moveTo(xx, yy);
      else ctx.lineTo(xx, yy);
    }
    ctx.closePath();
    ctx.globalAlpha = 0.9;
    ctx.strokeStyle = "#38bdf8";
    ctx.lineWidth = 2.5;
    ctx.stroke();

    //안쪽 글로우
    ctx.beginPath();
    ctx.arc(cxp, cyp, R - 6, 0, Math.PI * 2);
    ctx.globalAlpha = 0.15;
    ctx.fillStyle = "#38bdf8";
    ctx.fill();

    ctx.globalAlpha = 1;
  }

  //플레이어 본체
  drawPlayer(t);

  //레벨에 따른 화면 비네트
  drawVignetteIfNeeded();

  //충돌 시 화면 어둡게 플래시
  if (S.hitFlashTime > 0) {
    ctx.save();
    ctx.fillStyle = "rgba(0,0,0,0.35)";
    ctx.fillRect(0, 0, w, h);
    ctx.restore();
  }

  ctx.restore();

  //gimmicks 확장 훅
  gimmicks.onDraw(S.level, ctx, w, h, t);
}
