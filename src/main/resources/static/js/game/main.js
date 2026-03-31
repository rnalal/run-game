/*
게임 메인 루프 & 초기화 진입점
- 버튼,입력 바인딩
- tick()에서 물리,레벨,충돌,렌더링 한 번에 처리
*/
// main.js

import { startScreenShake } from "./effects.js";
import { gimmicks } from "/js/gimmicks.js";
import { enqueueEvent } from "./session.js";

import { S } from "./state.js";
import {
  startOverlay, gameOverOverlay, startBtn, restartBtn,
  levelUpNoticeEl, endBtn,
  finalScoreEl, finalCoinsEl, finalLevelEl, gameOverTitleEl
} from "./dom.js";

import { startSession, sendGameOver } from "./session.js";
import { bindInput } from "./input.js";
import { updateHUD } from "./hud.js";
import { spawnThings, pickCoins, pickWindBoost } from "./spawn.js";
import { checkObstacleCollision } from "./collision.js";
import { updateLevelByDistance, checkMissionClear } from "./level.js";
import { draw } from "./render/draw.js";
import { nowMs } from "./utils.js";
import { groundY, gravity, baseSpeed, sprintMul, reverseMul, MAX_LEVEL, LEVEL_UP_DIST } from "./constants.js";
import { isSpeedBoostActive } from "./powerup.js";

//WORLE PROGRESS ELEMENTS
//상단 진행 바 DOM 캐시
const worldFill = document.getElementById("worldFill");
const levelNodes = Array.from(
  document.querySelectorAll("#worldBar .level-node")
);
const playerMarker = document.getElementById("playerMarker");

//PAUSE,RESUME
//일시정지,재개 상태 관리 + 오버레이
function pauseGame() {
  if (S.gameOver || S.paused) return;

  S.paused = true;
  S.pausedSince = nowMs();

  document.body.classList.add("paused");
  document.getElementById("pauseOverlay").style.display = "flex";
}

function resumeGame() {
  if (!S.paused) return;

  const pausedFor = nowMs() - S.pausedSince;
  S.pausedAccum += pausedFor;

  S.paused = false;
  S.pausedSince = null;

  document.body.classList.remove("paused");
  document.getElementById("pauseOverlay").style.display = "none";
}

//WORLE PROGRESS LOGIC
//플레이어 x좌표 -> 전체 진행률로 변환
const TOTAL_DISTANCE = LEVEL_UP_DIST * MAX_LEVEL;

function updateWorldProgress() {
  if (!worldFill) return;

  const ratio = Math.min(1, S.playerX / TOTAL_DISTANCE);
  const percent = ratio * 100;

  worldFill.style.width = `${percent}%`;

  if(playerMarker) {
    playerMarker.style.left = `${percent}%`;
  }

  //레벨 노드 활성화
  levelNodes.forEach((node) => {
    const text = node.textContent.trim();

    if (text === "START") return;

    if (text === "GOAL") {
      node.classList.toggle("active", S.level >= MAX_LEVEL);
      return;
    }

    const lv = Number(text);
    if (!isNaN(lv)) {
      node.classList.toggle("active", S.level >= lv);
    }
  });
}

//GAME OVER
//종료 연출 + 요약 수치 표시
function showGameOverOverlay() {
  gameOverTitleEl.textContent =
    S.lastGameOverReason === "mission_clear"
      ? "🎉 Mission Clear!"
      : "🏁 Game Over";

  finalScoreEl.textContent = S.uiScore;
  finalCoinsEl.textContent = S.uiCoins;
  finalLevelEl.textContent = S.level;

  gameOverOverlay.style.display = "flex";
}

function doGameOver(reason) {
  return sendGameOver(reason, showGameOverOverlay);
}

//LEVEL UP TOAST
//레벨업 시 잠깐 뜨는 토스트
function showLevelUp() {
  if (!levelUpNoticeEl) return;

  levelUpNoticeEl.textContent = `LEVEL ${S.level}`;
  levelUpNoticeEl.classList.add("show");

  setTimeout(() => {
    levelUpNoticeEl.classList.remove("show");
  }, 900);
}

//BUTTON BINDING
//시작,재시작,종료,일시정지 버튼
startBtn.addEventListener("click", () => {
  startOverlay.style.display = "none";
  startSession();
});

restartBtn.addEventListener("click", () => {
  gameOverOverlay.style.display = "none";
  startOverlay.style.display = "flex";
});

endBtn.addEventListener("click", () => {
  doGameOver("manual_quit");
});

document.getElementById("pauseBtn")
  ?.addEventListener("click", pauseGame);

document.getElementById("resumeBtn")
  ?.addEventListener("click", resumeGame);

//INPUT
//키보드 입력 바인딩
bindInput();

/*
GAME LOOP
- requestAnimationFrame 기반 메인 루프
- 물리,충돌,스폰 -> HUD,진행바 -> draw()
*/
let lastTS = 0;

function tick(ts) {
  requestAnimationFrame(tick);

  //게임 종료, 일시정지, 세션 없음 -> 렌더만 유지
  if (S.gameOver || S.paused || !S.sessionId) {
    draw();
    return;
  }

  let dt = ts - (lastTS || ts);
  lastTS = ts;

  //너무 작은,큰 프레임 간격은 보정
  dt = Math.max(12, Math.min(20, dt));
  S.lastDeltaMs = dt;

  //피격 후 잠깐 멈추는 구간
  if(S.hitStopTime > 0) {
    S.hitStopTime -= dt;
    if(S.hitStopTime < 0) S.hitStopTime = 0;

    //로직은 멈추고 화면은 유지
    draw();
    return;
  }

  //히트 플래시 시간 감소
  if (S.hitFlashTime > 0) {
    S.hitFlashTime -= dt;
    if (S.hitFlashTime < 0) S.hitFlashTime = 0;
  }

  //화면 흔들림 시간 감소
  if(S.shakeTime > 0) {
    S.shakeTime -= dt;
    if(S.shakeTime < 0) S.shakeTime = 0;
  }

  //미끄러짐 상태 유지,종료
  const now = ts;
  if (S.player.slip) {
    S.player.slipTimeLeft -= dt;
    if (S.player.slipTimeLeft <= 0) {
      S.player.slip = false;
      S.player.slipTimeLeft = 0;
      S.slideActive = false;
    }
  }

  //거리 기반 레벨업 + 연출
  updateLevelByDistance(() => {
    showLevelUp();
    startScreenShake(400, 6);
  });

  //기본 속도 + 레벨 보정
  let speed = baseSpeed * (1 + (S.level - 1) * 0.09);
  //레벨별 기믹 가중치
  speed = gimmicks.onTick(S.level, speed);

  //스프린트,역주행,부스트 적용
  if (S.keyState.sprint) speed *= sprintMul;
  if (S.keyState.reverse) speed *= reverseMul;
  if (isSpeedBoostActive()) speed *= 2.7;

  //플레이어 월드 X 좌표 이동
  S.playerX += speed;
  if (S.playerX < 0) S.playerX = 0;

  //중력 + 점프,낙하
  S.player.vy += gravity;
  S.player.y += S.player.vy;

  //슬라이딩 중일 때 키 작은 실루엣으로
  const targetH = S.keyState.slide ? 26 : 44;
  S.player.h += (targetH - S.player.h) * 0.3;

  //지면 충돌 처리
  if (S.player.y + S.player.h >= groundY) {
    S.player.y = groundY - S.player.h;
    S.player.vy = 0;
    S.player.onGround = true;
  } else {
    S.player.onGround = false;
  }

  //스폰,픽업,충돌,미션 체크
  spawnThings();
  pickCoins();
  pickWindBoost();
  checkObstacleCollision(doGameOver);
  checkMissionClear(doGameOver);

  //상단 진행바 갱신 + 렌더
  updateWorldProgress();
  draw();
}

//첫 프레임 초기 렌더
requestAnimationFrame(tick);
updateHUD();
draw();
