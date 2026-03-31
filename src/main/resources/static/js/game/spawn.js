//spawn.js
/*
코인,체크포인트,장애물,바람 아이템 생성 & 픽업 처리
- 플레이어 진행 거리를 기준으로 다음 스폰 지점까지 도달하면 새 오브젝트 생성
- 레벨에 따라 장애물 등장 확률,간격 조정
- 자석,점수 2배,바람 부스트 같은 파워업 효과를 반영해서 코인,바람 아이템 획득 처리
- 코인,바람 픽업 시 서버로 이벤트 전송
*/
import { gimmicks } from "/js/gimmicks.js";

import { S } from "./state.js";
import { groundY, WIND_BOOST_DURATION_MS } from "./constants.js";
import { nowMs, worldX } from "./utils.js";
import { updateHUD } from "./hud.js";
import { enqueueEvent } from "./session.js";
import { isMagnetActive } from "./powerup.js";

//플레이어가 nextSpawnX를 지나가면
//코인,체크포인트,장애물,바람 아이템을 한 묶음으로 스폰
export function spawnThings(){
  if (S.playerX < S.nextSpawnX) return;

  //기본 스폰 기준점(플레이어보다 어느 정도 앞에 등장)
  const base = S.playerX + 320;

  //코인 3개
  for(let i=0;i<3;i++){
    S.coins.push({ x: base + i*30, y: groundY-60, r:8, taken:false });
  }

  //체크포인트(시각용)
  S.cpMarkers.push({ x: base + 150 });

  //레벨별 장애물 등장 확률 계산
  let p;
  if (S.level <= 6) {
    p = 0.45 + (S.level-1) * 0.05;
  } else {
    p = 0.73 + (S.level-6) * 0.03;
  }
  p = Math.min(0.68, p);

  /*
  레벨별 연출에서 선행 스폰
  - puddle, 특수 나무 등
  */
  const spawnResult = gimmicks.onSpawn(
    S.level,
    S.obstacles,
    S.coins,
    S.playerX,
    groundY
  );

  //puddle,tree가 이미 생성된 경우에는 기본 tree 스폰 제한
  if (!spawnResult.spawnedPuddle && !spawnResult.spawnedTree) {
    if (Math.random() < p) {
      const spawnX = base + 120;

      //기존 tree와 너무 가까우면 피하기
      const conflict = S.obstacles.some(ob =>
        ob.type === "tree" && Math.abs(ob.x - spawnX) < 120
      );

      //tree,noTreeZone 구간은 일정 거리 안에 또 생성 금지
      const treeConflict = S.obstacles.some(ob =>
        (ob.type === "tree" || ob.type === "noTreeZone") &&
        Math.abs(ob.x - spawnX) < 200
      );
      if (treeConflict) return;

      if (!conflict) {
        S.obstacles.push({
          type: "tree",
          x: spawnX,
          y: groundY - 34,
          w: 24,
          h: 34,
          collided: false
        });
      }
    }
  }

  /*
  바람 아이템
  - 레벨당 최대 3개까지만
  */
  if (S.windCountThisLevel < 3) {
    if (Math.random() < 0.05) {
      const windX = S.playerX + 550;
      S.windBoosts.push({
        x: windX,
        y: groundY - 100,
        r: 12,
        taken: false
      });
      S.windCountThisLevel++;
    }
  }

  //레벨이 올라갈수록 스폰 간격을 조금씩 줄여서 난이도 상승
  const levelFactor = 1 - (S.level-1)*0.04;
  const minStep = 260 * levelFactor;
  const maxStep = 420 * levelFactor;
  const step = minStep + Math.random()*(maxStep - minStep);
  S.nextSpawnX = S.playerX + step;
}

/*
코인 픽업 처리
- 자석 활성화 시 판정 범위 확장
- SCORE_X2 활성화 시 코인 획득 시점에서 점수 2배
- 서버로 coin_pick 이벤트 전송
*/
export function pickCoins(){
  const now = nowMs();
  for(const c of S.coins){
    if(c.taken) continue;
    const dx = Math.abs(worldX(c.x) - S.player.x);
    const dy = Math.abs(c.y - (S.player.y + S.player.h/2));

    const radiusMul = isMagnetActive() ? 2.4 : 1.0;

    if(dx < 16*radiusMul && dy < 26*radiusMul){
      c.taken = true;
      const isX2 = now <= S.scoreX2UntilMsClient;
      S.uiCoins += 1;
      S.uiScore += (isX2 ? 2 : 1);
      updateHUD();
      enqueueEvent("coin_pick",{ x: c.x, value: 1 });
    }
  }
}

/*
바람 아이템 픽업 처리
- SPEED_BOOST 지속 시간 연장
- 서버에는 powerup_pick(name=SPEED_BOOST)으로 기록만 남김
*/
export function pickWindBoost(){
  const now = nowMs();
  for (const w of S.windBoosts) {

    if (w.taken) continue;

    const dx = Math.abs(worldX(w.x) - (S.player.x + S.player.w/2));
    const dy = Math.abs(w.y - (S.player.y + S.player.h/2));

    if (dx < 20 && dy < 24) {
      w.taken = true;
      S.speedBoostUntilMs = now + WIND_BOOST_DURATION_MS;
      enqueueEvent("powerup_pick", {
        name: "SPEED_BOOST",
        durationMs: WIND_BOOST_DURATION_MS,
        x: S.playerX
      });
    }
  }
}
