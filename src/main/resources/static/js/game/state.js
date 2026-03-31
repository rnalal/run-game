//state.js
/*
전역 게임 상태 객체
- 런타임 동안 공유되는 상태를 한 곳에서 관리
- 다른 모듈에서 import { s } 후 S.xxx로 읽고 수정
*/

import { groundY, SHIELD_MAX_CHARGES, STAR_COUNT, WIND_LIMIT_PER_LEVEL, MAX_LEVEL } from "./constants.js";

//게임 전역 상태 S
export const S = {
  //게임 진행 플래그
  gameOver: false,
  enteredMissionLevel: false,
  slideActive: false,

  //레벨,바람 아이템
  level: 1,
  MAX_LEVEL,
  windCountThisLevel: 0,
  WIND_LIMIT_PER_LEVEL,

  //세션,시간 관리
  sessionId: null,
  startedAt: null,
  paused: false,
  pausedSince: null,
  pausedAccum: 0,

  //미션 상태
  lastGameOverReason: null,
  level10StartMs: null,
  missionCleared: false,

  //입력 상태
  keyState: { sprint:false, reverse:false, slide:false },

  //플레이어(토끼)
  playerX: 0,
  player: {
    x: 110,
    y: groundY - 44,
    w: 28,
    h: 44,
    vy: 0,
    onGround: true,
    sliding:false,
    slip:false, //startSession에서 함께 초기화
    slipTimeLeft: 0
  },

  //이벤트,점수,전송 큐
  seq: 0,
  queue: [],
  batchTimer: null,

  uiScore: 0,
  uiCoins: 0,
  lastGain: 0,

  //쉴드,파워업
  shieldCharges: SHIELD_MAX_CHARGES,
  shieldActiveUntilMs: -1,

  magnetUntilMs: -1,
  speedBoostUntilMs: -1,
  scoreX2UntilMsClient: -1,

  //라이프
  hitsSoFar: 0,

  //월드 오브젝트
  coins: [],
  cpMarkers: [],
  obstacles: [],
  windBoosts: [],

  nextSpawnX: 400,

  //배경(별)
  stars: Array.from({length: STAR_COUNT}, () => ({
    x: Math.random(),
    y: Math.random() * 0.5,
    s: 0.4 + Math.random() * 0.9
  })),

  //연출(화면 흔들림, 히트 스톱)
  shakeTime: 0,
  shakeIntensity: 0,

  hitStopTime: 0,
  hitFlashTime: 0,

  //마지막 프레임 경과 시간 - 몇몇 연출에 사용
  lastDeltaMs: 16
};
