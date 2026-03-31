// 장애물/코인/아이템 렌더
//objects.js

import { gimmicks } from "/js/gimmicks.js";

import { S } from "../state.js";
import { ctx, W } from "../dom.js";
import { groundY } from "../constants.js";
import { worldX } from "../utils.js";
import { drawWindIcon } from "./draw.js"; //바람 부스트 아이콘은 draw.js에서 공용 사용

/*
화면에 등장하는 오브젝트 렌더 담당
- 체크포인트 라인
- 장애물(슬라이드 장애물, 물웅덩이, 몬스터, 나무 등)
- 코인
- 바람 부스트 아이템
*/

//체트포인트 렌더
export function drawCheckpoints(){
  const w = W();
  ctx.strokeStyle = "#fbbf24";
  ctx.lineWidth = 3;

  for(const cp of S.cpMarkers){
    const x = worldX(cp.x);
    if (x < -10 || x > w+10) continue;

    ctx.beginPath();
    ctx.moveTo(x, groundY-60);
    ctx.lineTo(x, groundY);
    ctx.stroke();
  }
}

//장애물 렌더(타입별로)
export function drawObstacles(t){
  const w = W();

  for (const ob of S.obstacles) {
    //이미 충돌 처리된 장애물은 스킵
    if (ob.collided) continue;

    //트리 금지 구간 마커는 렌더 X
    if (ob.type === "noTreeZone") continue;

    //기믹에 따른 움직임 적용(흔들림, 점프 등)
    gimmicks.applyObstacleMovement(S.level, ob, t);

    const x = worldX(ob.x);
    if (x < -50 || x > w+50) continue;

    //슬라이드 장애물
    if (ob.type === "slideObstacle") {
      const barY = ob.y;
      const barH = ob.h;
      const barW = ob.w;

      //천장에서 내려오는 애니메이션
      if (ob.y < ob.targetY) {
        ob.y += ob.dropSpeed;
        if (ob.y > ob.targetY) ob.y = ob.targetY;
      }

      //양쪽 기둥
      ctx.fillStyle = "#4b5563";
      ctx.fillRect(x - 6, groundY - 80, 10, 80);
      ctx.fillRect(x + barW - 4, groundY - 80, 10, 80);

      //중앙 바
      ctx.fillStyle = "#111827";
      ctx.fillRect(x, barY, barW, barH);

      //안쪽 노란+검정 경고 스트라이프
      const stripePadding = 4;
      const innerX = x + stripePadding;
      const innerY = barY + stripePadding;
      const innerW = barW - stripePadding * 2;
      const innerH = barH - stripePadding * 2;

      const stripeWidth = 10;
      for (let sx = 0; sx < innerW + stripeWidth; sx += stripeWidth) {
        ctx.fillStyle = (Math.floor(sx / stripeWidth) % 2 === 0)
          ? "#facc15"
          : "#111827";
        ctx.beginPath();
        ctx.moveTo(innerX + sx, innerY);
        ctx.lineTo(innerX + sx + stripeWidth, innerY);
        ctx.lineTo(innerX + sx + stripeWidth - 6, innerY + innerH);
        ctx.lineTo(innerX + sx - 6, innerY + innerH);
        ctx.closePath();
        ctx.fill();
      }

      //바 주변 글로우
      ctx.save();
      ctx.globalAlpha = 0.22;
      ctx.fillStyle = "#facc15";
      ctx.fillRect(x - 4, barY - 4, barW + 8, barH + 8);
      ctx.restore();

      continue;
    }

    //물웅덩이 장애물
    if (ob.type === "puddle") {
      const px = x + ob.w/2;
      ctx.beginPath();
      ctx.ellipse(px, groundY - 4, ob.w/2, 8, 0, 0, Math.PI*2);

      const puddleGrad = ctx.createLinearGradient(0, groundY-10, 0, groundY);
      puddleGrad.addColorStop(0, "#93c5fd");
      puddleGrad.addColorStop(1, "#3b82f6");

      ctx.fillStyle = puddleGrad;
      ctx.fill();
      continue;
    }

    //거대 장애물
    if (ob.type === "giant") {
      ctx.fillStyle = "#7c3aed";
      ctx.fillRect(x, ob.y, ob.w, ob.h);
      continue;
    }

    //몬스터 장애물
    if (ob.type === "monster") {
      //몸통
      ctx.fillStyle = "#7f1d1d";
      ctx.beginPath();
      ctx.ellipse(x + ob.w/2, ob.y + ob.h/2, 14, 16, 0, 0, Math.PI*2);
      ctx.fill();

      //눈 흰자
      ctx.fillStyle = "white";
      ctx.beginPath();
      ctx.arc(x + ob.w/2 - 5, ob.y + ob.h/2 - 4, 4, 0, Math.PI*2);
      ctx.arc(x + ob.w/2 + 5, ob.y + ob.h/2 - 4, 4, 0, Math.PI*2);
      ctx.fill();

      //눈동자
      ctx.fillStyle = "black";
      ctx.beginPath();
      ctx.arc(x + ob.w/2 - 5, ob.y + ob.h/2 - 4, 2, 0, Math.PI*2);
      ctx.arc(x + ob.w/2 + 5, ob.y + ob.h/2 - 4, 2, 0, Math.PI*2);
      ctx.fill();

      //입
      ctx.strokeStyle = "#fca5a5";
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.arc(x + ob.w/2, ob.y + ob.h/2 + 5, 6, 0, Math.PI);
      ctx.stroke();

      continue;
    }

    //기본 나무 장애물
    ctx.fillStyle = ob.collided ? "#7c2d12" : "#b45309";
    ctx.fillRect(x + ob.w/2 - 4, ob.y + 10, 8, ob.h - 10);

    ctx.beginPath();
    ctx.fillStyle = ob.collided ? "#4b5563" : "#22c55e";
    const leafY = ob.y + 8;
    ctx.arc(x + ob.w/2, leafY, 14, 0, Math.PI * 2);
    ctx.arc(x + ob.w/2 - 12, leafY + 4, 11, 0, Math.PI * 2);
    ctx.arc(x + ob.w/2 + 12, leafY + 4, 11, 0, Math.PI * 2);
    ctx.fill();
  }
}

//코인 렌더
export function drawCoins(){
  const w = W();
  for(const c of S.coins){
    if(c.taken) continue;
    const x = worldX(c.x);
    if (x < -20 || x > w+20) continue;
    ctx.beginPath();
    ctx.arc(x,c.y,c.r,0,Math.PI*2);
    const coinGrad = ctx.createRadialGradient(x-3,c.y-3,2,x,c.y,c.r);
    coinGrad.addColorStop(0,"#fff7cc");
    coinGrad.addColorStop(1,"#facc15");
    ctx.fillStyle = coinGrad;
    ctx.fill();
    ctx.strokeStyle="#eab308";
    ctx.stroke();
  }
}

//바람 부스트 아이템 렌더
export function drawWindBoosts(t){
  const w = W();
  for (const wb of S.windBoosts) {
    if (wb.taken) continue;
    const x = worldX(wb.x);
    if (x < -30 || x > w+30) continue;
    drawWindIcon(x, wb.y, wb.r, t);
  }
}
