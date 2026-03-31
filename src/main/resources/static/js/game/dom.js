//document.getElementById 모음
//dom.js

//오버레이,버튼 관련 엘리먼트
export const startOverlay    = document.getElementById("startOverlay"); //게임 시작 전 오버레이
export const gameOverOverlay = document.getElementById("gameOverOverlay"); //게임 오버 오버레이
export const startBtn        = document.getElementById("startBtn"); //시작 버튼
export const restartBtn      = document.getElementById("restartBtn"); //다시하기 버튼
export const levelUpNoticeEl = document.getElementById("levelUpNotice"); //레벨업 안내 문구

//캔버스,컨텍스트,크기 헬퍼
export const canvas = document.getElementById("game"); //게임 캔버스
export const ctx    = canvas.getContext("2d"); //2D 렌더링 컨텍스트
export const W = () => canvas.width; //현재 켄버스 너비
export const H = () => canvas.height; //현재 캔버스 높이

//HUD(상태표시) 영역
export const scoreStateEl  = document.getElementById("scoreState"); //점프 표시
export const coinsStateEl  = document.getElementById("coinsState"); //코인 수 표시
export const livesStateEl  = document.getElementById("livesState"); //남은 목숨,히트 수 표시
export const levelStateEl  = document.getElementById("levelState"); //현재 레벨 표시
export const shieldStateEl = document.getElementById("shieldState"); //쉴드 상태 표시
export const endBtn        = document.getElementById("endBtn"); //수동 종료 버튼(게임 끝내기)

//게임 오버 결과 영역
export const finalScoreEl  = document.getElementById("finalScore"); //최종 점수
export const finalCoinsEl  = document.getElementById("finalCoins"); //최종 코인
export const finalLevelEl  = document.getElementById("finalLevel"); //도달 레벨
export const gameOverTitleEl = document.getElementById("gameOverTitle"); //게임 오버 제목,메세지
