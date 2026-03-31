// 토끼 렌더
//player.js

import { S } from "../state.js";
import { ctx } from "../dom.js";
import { groundY } from "../constants.js";
import { isSpeedBoostActive, isShieldActive } from "../powerup.js";

/*
플레이어 캐릭터 렌더링 모듈
- 토끼 본체 드로잉
- 그림자 드로잉
- 상태(점프,슬라이드)에 따른 각도,모양 변경
*/

//토끼 본체(자세,기울기 포함)
export function drawRabbit(x, y, w, h, keys, onGround, t) {
  const cx = x + w/2;
  const cy = y + h/2;

  //이동 상태에 따라 캐릭터 기울기 조정
  let tilt = 0;
  if (!onGround) {
    tilt = -0.08;   //점프 중 살짝 위로
  } else if (keys.sprint || isSpeedBoostActive()) {
    tilt = -0.05;   //대쉬,부스트 시 앞으로 기울임
  } else if (keys.reverse) {
    tilt = 0.07;    //뒤로 달릴 때 약간 뒤로
  }

  //미끄러짐 상태면 흔들리는 모션
  if(S.player.slip) {
    tilt = 0.25 * Math.sin(t/50);
  }

  ctx.save();
  ctx.translate(cx, cy);
  ctx.rotate(tilt);

  const bodyHeight = h * (keys.slide ? 0.6 : 0.9); //슬라이드 시 몸 낮추기
  const bodyWidth  = w * 1.0;

  //몸통
  ctx.fillStyle = "#e5e7eb";
  ctx.beginPath();
  ctx.ellipse(0, bodyHeight*0.1, bodyWidth/2, bodyHeight/2, 0, 0, Math.PI*2);
  ctx.fill();

  //머리
  const headW = w * 0.9;
  const headH = h * 0.6;
  ctx.fillStyle = "#f9fafb";
  ctx.beginPath();
  ctx.ellipse(headW*0.1, -bodyHeight*0.4, headW/2, headH/2, 0, 0, Math.PI*2);
  ctx.fill();

  //귀(양쪽)
  const earHeight = headH * 0.9;
  const earWidth  = headW * 0.25;

  function drawEar(offsetX) {
    ctx.save();
    ctx.translate(headW*0.1 + offsetX, -bodyHeight*0.4 - earHeight*0.5);
    ctx.beginPath();
    ctx.fillStyle = "#e5e7eb";
    ctx.ellipse(0,0, earWidth/2, earHeight/2, 0, 0, Math.PI*2);
    ctx.fill();

    ctx.beginPath();
    ctx.fillStyle = "#fecaca";
    ctx.ellipse(0,0, (earWidth/2)*0.6, (earHeight/2)*0.7, 0, 0, Math.PI*2);
    ctx.fill();
    ctx.restore();
  }
  drawEar(-earWidth*0.4);
  drawEar( earWidth*0.2);

  //눈
  ctx.fillStyle = "#0f172a";
  ctx.beginPath();
  ctx.arc(headW*0.2, -bodyHeight*0.45, 2, 0, Math.PI*2);
  ctx.fill();

  //코,입 주변 포인트
  ctx.fillStyle = "#f97373";
  ctx.beginPath();
  ctx.arc(headW*0.3, -bodyHeight*0.4, 2, 0, Math.PI*2);
  ctx.fill();

  //꼬리
  ctx.fillStyle = "#f9fafb";
  ctx.beginPath();
  ctx.arc(-bodyWidth*0.5, bodyHeight*0.1, 5, 0, Math.PI*2);
  ctx.fill();

  ctx.restore();
}

//플레이어 발밑 그림자
export function drawPlayerShadow(){
  const p = S.player;
  ctx.globalAlpha = 0.28;
  ctx.fillStyle = "#000000";
  const shadowWidth = p.w * 1.6;
  ctx.beginPath();
  ctx.ellipse(
    p.x + p.w/2,
    groundY + 6,
    shadowWidth/2,
    7,
    0,
    0,
    Math.PI*2
  );
  ctx.fill();
  ctx.globalAlpha = 1;
}

//플레이어 전체 렌더(그림자 -> 토끼)
export function drawPlayer(t){
  const p = S.player;
  drawPlayerShadow();
  drawRabbit(p.x, p.y, p.w, p.h, S.keyState, p.onGround, t);
}
