//화면 흔들림 효과 관련 유틸
// effects.js
import { S } from "./state.js";

//일정 시간 동안 화면을 흔들리게 하는 효과
//durationMs: 흔들리는 시간
//intensity: 흔들림 강도
export function startScreenShake(durationMs, intensity = 4) {
  S.shakeTime = durationMs;
  S.shakeIntensity = intensity;
}
