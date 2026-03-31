//파워업 로직
//powerup.js

import { S } from "./state.js";
import { nowMs } from "./utils.js";
import { updateHUD } from "./hud.js";
import { enqueueEvent } from "./session.js";

//파워업 남은 시간 체크
export function isMagnetActive() {
  return nowMs() <= S.magnetUntilMs;
}
export function isShieldActive() {
  return nowMs() <= S.shieldActiveUntilMs;
}
export function isSpeedBoostActive() {
  return nowMs() <= S.speedBoostUntilMs;
}

//파워업 사용 처리 + 서버 이벤트 전송
export function sendPowerupPick(name, durationMs) {
  if (!S.sessionId || !name) return;

  const now = nowMs();
  const dur = Math.max(1000, durationMs || 6000);

  if (name === "SCORE_X2") {
    //점수 2배 유지 시간 클라 쪽에서만 관리
    S.scoreX2UntilMsClient = Math.max(S.scoreX2UntilMsClient, now + dur);
  } else if (name === "MAGNET") {
    //코인 자석 효과
    S.magnetUntilMs = now + dur;
  } else if (name === "SHIELD") {
    //쉴드는 남은 횟수 소모 후 발동
    if (S.shieldCharges <= 0) return;
    S.shieldCharges--;
    S.shieldActiveUntilMs = now + dur;
  }

  updateHUD();

  enqueueEvent("powerup_pick", {
    name,
    durationMs: dur,
    x: S.playerX
  });
}
