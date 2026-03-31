//utils.js
/*
- 게임 경과 시간 계산
- 월드/화면 좌표 변환
- 색상 보간 관련 헬퍼
*/

import { S } from "./state.js";

//일시정지까지 고려한 게임 진행 시간
export function nowMs(){
  if(!S.startedAt) return 0;

  const raw = Date.now()-S.startedAt;   //게임 시작 이후 실제 경과 시간
  const pausedExtra = S.paused ? (Date.now()-S.pausedSince) : 0;    //현재 일시정지 중이면 그 구간 제외

  //누적 ㅇ리시정지 시간 + 현재 일시정지 중인 시간 빼기
  return Math.max(0, raw - S.pausedAccum - pausedExtra);
}

//사각형 A/B 충돌 여부
export function rectOverlap(a,b){
  return (
    a.x < b.x + b.w &&
    a.x + a.w > b.x &&
    a.y < b.y + b.h &&
    a.y + a.h > b.y
  );
}

//월드 X좌표 -> 화면 X좌표로 변환
//플레이어를 기준으로 카메라가 따라가는 구조
export function worldX(wx){
  return S.player.x + (wx - S.playerX);
}

//색상 헬퍼
export function hexToRgb(hex) {
  hex = hex.replace("#","");
  const num = parseInt(hex, 16);
  return {
    r: (num >> 16) & 255,
    g: (num >> 8) & 255,
    b: num & 255
  };
}

//두 색상 사이를 t(0~1) 비율로 보간해서 rgb() 문자열 반환
export function mixHex(hex1, hex2, t) {
  const c1 = hexToRgb(hex1);
  const c2 = hexToRgb(hex2);
  const r = Math.round(c1.r + (c2.r - c1.r)*t);
  const g = Math.round(c1.g + (c2.g - c1.g)*t);
  const b = Math.round(c1.b + (c2.b - c1.b)*t);
  return `rgb(${r},${g},${b})`;
}
