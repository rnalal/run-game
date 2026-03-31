// 충돌 판정
//collision.js

import { startScreenShake } from "./effects.js";
import { S } from "./state.js";
import { rectOverlap, nowMs } from "./utils.js";
import { updateHUD } from "./hud.js";
import { enqueueEvent } from "./session.js";
import { isShieldActive, isSpeedBoostActive } from "./powerup.js";

/*
장애물 충돌 처리
- hit 연출 + 점수 감소 + 게임오버 조건 관리
- SPEED_BOOST/SHIELD 상태에 따라 처리
*/
export function sendHitObstacle(sendGameOverFn){
  const impact = Math.floor(5 + Math.random()*6); //서버로 보낼 충돌 강도

  //SPEED_BOOST 중이면 데미지 없이 이벤트만 기록
  if(isSpeedBoostActive()) {
    enqueueEvent("hit_obstacle", { impact, ignoredByBoost:true });
    return;
  }

  //기본 충돌 연출
  S.hitStopTime = 180;
  S.hitFlashTime = 120;
  startScreenShake(500, 8);

  //살짝 뒤로 밀리고 튕겨 오르는 느낌
  S.playerX -= 18;
  S.player.vy = -6;

  enqueueEvent("hit_obstacle",{ impact });

  //SHIELD 활성화 상태면 연출만 하고 데미지 없음
  if (isShieldActive()) {
    S.shieldActiveUntilMs = -1; //쉴드 소모
    updateHUD();
    return;
  }

  //실제 데미지 처리
  S.hitsSoFar++;
  S.uiScore = Math.max(0, S.uiScore - 10);
  updateHUD();

  //일정 횟수 이상 충돌 시 게임 오버
  if (S.hitsSoFar >= 3) {
    sendGameOverFn("hits_exceeded");
  }
}

/*
장애물 충돌 체크
- 플레이어 vs 장애물 AABB 충돌
- 타입별(puddle, slideObstacle, 일반 나무 등) 처리
*/
export function checkObstacleCollision(sendGameOverFn){
  const playerRect = {
    x: S.playerX,
    y: S.player.y,
    w: S.player.w,
    h: S.player.h
  };

  for (const ob of S.obstacles) {
    if(ob.collided) continue; //이미 처리된 장애물은 스킵
    if (ob.type === "noTreeZone") continue;

    const obRect = { x: ob.x, y: ob.y, w: ob.w, h: ob.h };

    if(rectOverlap(playerRect, obRect)) {

      //물웅덩이 -> 슬립 상태 + slide_start 이벤트
      if(ob.type === "puddle") {

        //이미 미끄러지는 중이면 중복 처리 방지
        if(S.player.slip) return;

        ob.collided = true;

        //slide_start는 최초 1회만 보내기
        if (!S.slideActive) {
            S.slideActive = true;
            enqueueEvent("slide_start", { reason: "puddle" });
        }

        S.player.slip = true;
        S.player.slipTimeLeft = 1200;
        startScreenShake(700, 6);

        return;
      }

      //슬라이드 전용 장애물
      if (ob.type === "slideObstacle") {
        //슬라이드 중이면 그냥 통과
        if (S.keyState.slide || S.player.sliding) {
          ob.collided = true;
          return;
        }
        //슬라이드 안 하면 일반 hit 처리
        ob.collided = true;
        sendHitObstacle(sendGameOverFn);
        return;
      }

      //그 외 나무,몬스터 등 일반 장애물
      ob.collided = true;
      sendHitObstacle(sendGameOverFn);
      return;
    }
  }
}
