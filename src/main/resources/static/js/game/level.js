/*
레벨 계산 & 레벨업 처리
- 이동 거리 기준으로 현재 레벨 산출
- 레벨 변경 시 연출(gimmicks),HUD 연동
- 10레벨 미션 시작, 클리어 판정
*/
//level.js

import { gimmicks } from "/js/gimmicks.js";

import { S } from "./state.js";
import { LEVEL_UP_DIST, MAX_LEVEL, MISSION_LEVEL, MISSION_DURATION_MS } from "./constants.js";
import { nowMs } from "./utils.js";
import { updateHUD } from "./hud.js";

//플레이어 이동 거리 리준으로 레벨 재계산 + 레벨업 처리
export function updateLevelByDistance(onLevelUpUI) {
  const newLevel = Math.min(MAX_LEVEL, Math.floor(S.playerX / LEVEL_UP_DIST) + 1);

  //레벨이 바뀐 경우에만 처리
  if (newLevel !== S.level) {
    S.level = newLevel;
    //레벨별 연출,패턴 활성화
    gimmicks.activate(S.level);
    //레벨별 바람 아이템 사용 카운트 리셋
    S.windCountThisLevel = 0;

    //미션 레벨 최초 진입 시 타이머 세팅
    if(S.level === MISSION_LEVEL && !S.enteredMissionLevel) {
      S.enteredMissionLevel = true;
      S.level10StartMs = nowMs();
      S.missionCleared = false;
    }

    //HUD에 레벨 반영
    updateHUD();
    //UI 전용 레벨업 연출 콜백
    if (typeof onLevelUpUI === "function") onLevelUpUI();
  }
}

//미션 레벨에서 제한 시간 내 생존 여부 체크
export function checkMissionClear(sendGameOverFn) {
  if (S.level === MISSION_LEVEL && !S.missionCleared && S.level10StartMs !== null) {
    const elapsed = nowMs() - S.level10StartMs;

    //미션 시간 도달 시 클리어 처리
    if (elapsed >= MISSION_DURATION_MS) {
      S.missionCleared = true;
      sendGameOverFn("mission_clear");
    }
  }
}

