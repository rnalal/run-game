/*
HUD(상단 상태 UI) 갱신
- 점수,코인,레벨 표시
- 남은 목숨과 쉴드 아이콘 갱신
*/

import { S } from "./state.js";
import { MAX_HITS, SHIELD_MAX_CHARGES } from "./constants.js";
import { scoreStateEl, coinsStateEl, livesStateEl, levelStateEl, shieldStateEl } from "./dom.js";

export function updateHUD(){
  //기본 숫자 스탯
  scoreStateEl.textContent = S.uiScore;
  coinsStateEl.textContent = S.uiCoins;
  levelStateEl.textContent = S.level;

  //남은 목숨 -> 하트 아이콘으로 표현
  const remain = Math.max(0, MAX_HITS - S.hitsSoFar);
  livesStateEl.textContent =
    "❤️".repeat(remain) + "🤍".repeat(MAX_HITS-remain);

  //남은 쉴드 -> 방패 아이콘으로 표현
  const remainShield = Math.max(0, S.shieldCharges);
  shieldStateEl.textContent =
    "🛡️".repeat(remainShield) + "⬜".repeat(SHIELD_MAX_CHARGES - remainShield);
}
