//session.js
/*
세션 시작,종료 + 이벤트 배치 전송 담당
- 클라이언트에서 발생한 게임 이벤트를 큐에 쌓았다가 주기적으로 서버에 전송
- 세션 시작 시 플레이어,월드 상태 초기화 + 서버에 세션 생성 요청
- 게임 종료 시 game_over 이벤트 전송 후 /end API 호출로 서버 집계 트리거
- 연속 전송 실패 시 배치 타이머 중단
*/

import { gimmicks } from "/js/gimmicks.js";

import { S } from "./state.js";
import { updateHUD } from "./hud.js";
import { groundY, SHIELD_MAX_CHARGES } from "./constants.js";
import { nowMs } from "./utils.js";
import { resetJumpInput } from "./input.js";

/*
이벤트 큐에 한 건 추가
- tMs(이벤트 시각)는 단조 증가만 허용해서 시간 역행 방지
- payload는 서버로 보낼 때 JSON 문자열로 구정
*/
export function enqueueEvent(type, payload = {}) {
  if (!S.sessionId || S.gameOver) return;

  const t = nowMs();

  //역행 방지(같거나 과거인 타임스탬프는 무시)
  if (t <= S.lastEventTms) return;

  S.lastEventTms = t;

  S.queue.push({
    seq: ++S.seq,
    tMs: t,
    type,
    payloadJson: JSON.stringify(payload ?? {})
  });
}

/*
이벤트 배치를 서버로 전송
- 성공 시에만 큐에서 제거
- 연속 3회 실패 시 타이머 중단해서 무한 재시도 방지
*/
export async function sendBatch(){
  if (!S.sessionId || S.gameOver || S.queue.length === 0) return;

  const batch = { events: [...S.queue] };

  try {
    const res = await fetch(`/api/sessions/${S.sessionId}/events`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(batch),
      credentials: "include"
    });

    if (!res.ok) {
      S.batchFailCount++;

      console.warn("이벤트 배치 실패:", res.status);

      //연속 실패 시 전송 중단
      if (S.batchFailCount >= 3) {
        console.error("이벤트 정송 중단 (연속 실패)");
        clearInterval(S.batchTimer);
      }
      return; //실패 시 큐 유지
    }

    //성공했을 때만 제거
    S.batchFailCount = 0;
    S.queue.splice(0, batch.events.length);

  } catch (e) {
    S.batchFailCount++;
    console.error("이벤트 전송 실패:", e);
    //네트워크 실패 시도 큐 유지
  }
}

/*
세션 시작 요청 + 클라이언트 상태 초기화
- 이전 세션 타이머,큐 정리 후 /api/sessions/start 호출
- 응답으로 받은 sessionId를 S.sessionId에 저장
- 이후 1초마다 sendBatch() 실행
*/
export async function startSession() {

  resetJumpInput();
  S.gameOver = false;

  //이전 세션 관련 타이머,큐 완전 정리
  if(S.batchTimer){
    clearInterval(S.batchTimer);
    S.batchTimer = null;
  }

  S.sessionId = null;
  S.queue = [];
  S.seq = 0;
  S.batchFailCount = 0;
  S.lastEventTms = 0;

  //입력 상태 초기화
  S.keyState = {
    sprint: false,
    reverse: false,
    slide: false,
    jump: false
  };

  S.player.slip = false;
  S.player.slipTimeLeft = 0;
  S.slideActive = false;
  S.hitStopTime = 0;
  S.shakeTime = 0;

  try {
    const res = await fetch("/api/sessions/start", {
      method: "POST",
      headers: { "Content-Type":"application/json" },
      body: JSON.stringify({ level:"world-1", clientVersion:"1.0.0" }),
      credentials:"include"
    });

    if (!res.ok) {
      alert("로그인이 필요합니다!");
      window.location.href = "login.html";
      return;
    }

    const text = await res.text();
    S.sessionId   = text.replace(/"/g,"").trim();
    S.startedAt   = Date.now();
    S.paused      = false;
    S.pausedSince = null;
    S.pausedAccum = 0;

    S.level = 1;
    gimmicks.activate(1);

    //미션 관련 상태 초기화
    S.level10StartMs = null;
    S.missionCleared = false;
    S.lastGameOverReason = null;

    //기본 위치,속도,스코어 리셋
    S.playerX = 0;
    S.player.y = groundY - 44;
    S.player.vy = 0;
    S.player.onGround = true;
    S.player.sliding  = false;

    S.uiScore = 0;
    S.uiCoins = 0;
    S.lastGain = 0;
    S.hitsSoFar = 0;

    //파워업,쉴드 상태 리셋
    S.shieldCharges        = SHIELD_MAX_CHARGES;
    S.shieldActiveUntilMs  = -1;
    S.magnetUntilMs        = -1;
    S.speedBoostUntilMs    = -1;
    S.scoreX2UntilMsClient = -1;

    //월드 오브젝트 초기화
    S.coins.length      = 0;
    S.cpMarkers.length  = 0;
    S.obstacles.length  = 0;
    S.windBoosts.length = 0;
    S.nextSpawnX        = 400;

    S.seq   = 0;
    S.queue = [];

    S.lastEventTms = 0;     //tMs 단조 증가용
    S.batchFailCount = 0;   //연속 실패 카운터 초기화

    S.slideActive = false;

    //배치 타이머 시작
    if (S.batchTimer) clearInterval(S.batchTimer);
    S.batchTimer = setInterval(sendBatch,1000);

    updateHUD();
    enqueueEvent("game_start",{});

  } catch(e) {
    console.error(e);
  }
}

/*
게임 종료 처리 + 서버 세션 종료 호출
- 클라 쪽에서는 오버레이 먼저 띄우고 종료 연출
- 서버에는 game_over 이벤트 +/end 호출로 집계 마무리
*/
export async function sendGameOver(reason="manual", showGameOverOverlay) {
  if (S.gameOver) return;
  S.gameOver = true;
  S.lastGameOverReason = reason;

  //오버레이는 즉시
  if (typeof showGameOverOverlay === "function") showGameOverOverlay();

  //세션 없으면 여기서 끝
  if (!S.sessionId) return;

  //서버에는 이벤트만 먼저
  enqueueEvent("game_over",{ reason });
  await sendBatch();

  try {
    await fetch(`/api/sessions/${S.sessionId}/end`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ reason }),
      credentials: "include"
    });
  } catch (e) {
    console.warn("세션 종료 호출 실패(무시):", e);
  } finally {
    S.sessionId = null;
    if (S.batchTimer) {
        clearInterval(S.batchTimer);
        S.batchTimer = null;
    }
  }
}
