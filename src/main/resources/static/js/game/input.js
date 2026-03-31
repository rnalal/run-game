/*
키 입력 처리
- 방향키로 점프,질주,역주행,슬라이드 조작
- 점프 디바운스로 연타 방지
- M/S/X 키로 파워업 픽업 트리거
*/
//input.js

import { S } from "./state.js";
import { jumpVy, JUMP_DEBOUNCE } from "./constants.js";
import { nowMs } from "./utils.js";
import { enqueueEvent } from "./session.js";
import { sendPowerupPick } from "./powerup.js";

let lastJumpAt = 0;


//점프 입력 시간 초기화
//게임 재시작 시 사용
export function resetJumpInput() {
    lastJumpAt = 0;
}

export function bindInput(){
  //키 눌렀을 때
  window.addEventListener("keydown", e => {
    //세션 없으면 입력 무시
    if(!S.sessionId) return;

    //점프
    if(e.code==="ArrowUp"){
      const t = nowMs();
      //점프 디바운스: 일정 시간 내 재입력 무시
      if(t-lastJumpAt < JUMP_DEBOUNCE) return;
      lastJumpAt = t;

      if(S.player.onGround){
        S.player.vy = jumpVy;
        S.player.onGround = false;
        enqueueEvent("jump",{});
      }
    }

    //질주 시작
    if(e.code==="ArrowRight"){
      if(!S.keyState.sprint){
        S.keyState.sprint = true;
        enqueueEvent("sprint_start",{});
      }
    }

    //역주행 시작
    if(e.code==="ArrowLeft"){
      if(!S.keyState.reverse){
        S.keyState.reverse = true;
        enqueueEvent("reverse_start",{});
      }
    }

    //슬라이드 시작
    if(e.code==="ArrowDown"){
      if(!S.keyState.slide){
        S.keyState.slide = true;
        S.player.sliding = true;
        enqueueEvent("slide_start",{});
      }
    }

    //파워업 단축키
    if (e.code === "KeyM") sendPowerupPick("MAGNET");
    if (e.code === "KeyS") sendPowerupPick("SHIELD");
    if (e.code === "KeyX") sendPowerupPick("SCORE_X2");
  });

  //키 뗐을 때
  window.addEventListener("keyup", e => {
    if(!S.sessionId) return;

    //질주 종료
    if(e.code==="ArrowRight"){
      if(S.keyState.sprint){
        S.keyState.sprint=false;
        enqueueEvent("sprint_end",{});
      }
    }

    //역주행 종료
    if(e.code==="ArrowLeft"){
      if(S.keyState.reverse){
        S.keyState.reverse=false;
        enqueueEvent("reverse_end",{});
      }
    }

    //슬라이드 종료
    if(e.code==="ArrowDown"){
      if(S.keyState.slide){
        S.keyState.slide=false;
        S.player.sliding=false;
        enqueueEvent("slide_end",{});
      }
    }
  });
}
