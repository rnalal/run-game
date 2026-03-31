// 숫자 상수들
//constants.js

//미션 조건
export const MISSION_LEVEL = 10; //미션이 걸리는 목표 레벨
export const MISSION_DURATION_MS = 60000; //미션 제한 시간

//플레이어 물리 값
export const groundY = 260; //지면 Y 좌표
export const gravity = 0.6; //중력 가속도
export const jumpVy  = -11; //점프 시작 속도

//이동 속도 관련
export const baseSpeed   = 3.2; //기본 이동 속도
export const sprintMul   = 1.8; //스프린트 배속
export const reverseMul  = -0.7; //역주행 시 배속

//바람 아이템
export const WIND_BOOST_DURATION_MS = 6000; //WIND_BOOST 지속 시간
export const WIND_LIMIT_PER_LEVEL = 3; //레벨당 최대 WIND_BOOST 수

//레벨 업,게임 진행
export const MAX_LEVEL = 10; //최대 레벨
export const LEVEL_UP_DIST = 4500; //레벨업마다 필요한 누적 거리

//방어,피격 관련
export const SHIELD_MAX_CHARGES = 3; //쉴드 최대 충전 횟수
export const MAX_HITS  = 3; //피격 허용 횟수

//연출,입력 관련
export const STAR_COUNT = 50; //밤하늘 별 개수
export const JUMP_DEBOUNCE = 600; //점프 입력 딜레이

