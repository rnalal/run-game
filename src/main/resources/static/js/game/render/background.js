// 하늘/별/구름/지면
//background.js

import { S } from "../state.js";
import { ctx, W, H } from "../dom.js";
import { groundY, MAX_LEVEL } from "../constants.js";
import { mixHex } from "../utils.js";

/*
배경 전용 렌더 모듈
- 레벨에 따라 하늘,조명 분위기 변경
- 플레이어 X 위치 기준으로 구름,별 패럴렉스 이동
- 지면,언덕,도로 라인까지 한 번에 그리기
*/

//구름 덩어리
export function drawCloud(cx, cy, r) {
  ctx.fillStyle = "rgba(255,255,255,0.9)";
  ctx.beginPath();
  ctx.arc(cx, cy, r, 0, Math.PI*2);
  ctx.arc(cx+r*0.7, cy+5, r*0.8, 0, Math.PI*2);
  ctx.arc(cx-r*0.7, cy+8, r*0.7, 0, Math.PI*2);
  ctx.fill();
}

//밤 하늘 별 깜빡이는 효과 + 패럴렉스 이동
export function drawStars(t, w, h) {
  ctx.save();
  ctx.globalAlpha = 0.9;
  for (let i = 0; i < S.stars.length; i++) {
    const s = S.stars[i];

    //플레이어 X에 비례한 느린 스크롤
    const baseX = s.x * (w + 200);
    const x = (baseX - (S.playerX * 0.15 % (w + 200))) - 100;
    if (x < -20 || x > w+20) continue;

    const y = 20 + s.y * 160;
    const twinkle = 0.5 + 0.5 * Math.sin((t/250) + i * 13); //반짝임
    const radius = (0.7 + s.s * 0.8) * twinkle;

    ctx.beginPath();
    ctx.fillStyle = "rgba(248,250,252,0.95)";
    ctx.arc(x, y, radius, 0, Math.PI*2);
    ctx.fill();
  }
  ctx.globalAlpha = 1;
  ctx.restore();
}

//기본 배경(하늘 그라데이션,언덕,구름,도로)
export function drawBaseBackground(t){
  const w = W(), h = H();
  const levelT = (MAX_LEVEL > 1) ? (S.level-1)/(MAX_LEVEL-1) : 0;

  //레벨에 따라 낮->밤으로 하늘 색 점점 어두워짐
  const topColor    = mixHex("#aee3ff", "#020617", levelT);
  const midColor    = mixHex("#7bcef8", "#01040b", levelT);
  const bottomColor = mixHex("#d3efff", "#00020a", levelT);

  const skyGrad = ctx.createLinearGradient(0,0,0,h);
  skyGrad.addColorStop(0,   topColor);
  skyGrad.addColorStop(0.5, midColor);
  skyGrad.addColorStop(1,   bottomColor);
  ctx.fillStyle = skyGrad;
  ctx.fillRect(0,0,w,h);

  //일정 레벨 이상에서만 별 표시
  if (S.level >= 8) drawStars(t, w, h);

  //1단 언덕 실루엣
  ctx.fillStyle = "rgba(96,165,250,0.45)";
  const hillBaseY = groundY + 18;
  ctx.beginPath();
  ctx.moveTo(-50, hillBaseY);
  ctx.quadraticCurveTo(w*0.3, hillBaseY-70, w*0.7, hillBaseY);
  ctx.quadraticCurveTo(w*1.1, hillBaseY+40, w+50, hillBaseY);
  ctx.closePath();
  ctx.fill();

  //2단 언덕 실루엣
  ctx.fillStyle = "rgba(56,189,248,0.5)";
  ctx.beginPath();
  ctx.moveTo(-80, hillBaseY+10);
  ctx.quadraticCurveTo(w*0.4, hillBaseY-40, w*0.9, hillBaseY+10);
  ctx.lineTo(w+80,h);
  ctx.lineTo(-80,h);
  ctx.closePath();
  ctx.fill();

  //구름
  //플레이어 이동에 따라 천천히 흐르도록)
  const cloudOffset = (S.playerX * 0.1) % (w+200);
  drawCloud(100 - cloudOffset, 70, 38);
  drawCloud(360 - cloudOffset, 50, 32);
  drawCloud(720 - cloudOffset, 90, 40);

  //지면 경계 라인
  ctx.fillStyle="#3a3a3a";
  ctx.fillRect(0,groundY,w,4);

  //도로 영역 그라데이션
  const groundGrad = ctx.createLinearGradient(0,groundY,0,h);
  groundGrad.addColorStop(0,"#e5e7eb");
  groundGrad.addColorStop(1,"#cbd5e1");
  ctx.fillStyle = groundGrad;
  ctx.fillRect(0,groundY+4,w,h-groundY-4);

  //중앙 차선 점선
  const laneOffset = (S.playerX * 0.3) % 40;
  ctx.strokeStyle = "#9ca3af";
  ctx.lineWidth = 2;
  ctx.setLineDash([18,18]);
  ctx.beginPath();
  ctx.moveTo(-laneOffset, groundY+28);
  ctx.lineTo(w, groundY+28);
  ctx.stroke();
  ctx.setLineDash([]);
}

//고레벨에서 화면 가장자리 어둡게
export function drawVignetteIfNeeded(){
  const w = W(), h = H();
  if (S.level >= 9) {
    ctx.save();
    const gradMid = ctx.createRadialGradient(
      w/2, h*0.4, w*0.15,
      w/2, h*0.4, w*0.9
    );
    gradMid.addColorStop(0, "rgba(0,0,0,0)");
    gradMid.addColorStop(1, "rgba(0,0,0,0.55)");
    ctx.fillStyle = gradMid;
    ctx.fillRect(0, 0, w, h);
    ctx.restore();
  }
}
