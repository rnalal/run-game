package com.example.rungame.qna.domain;

//QnA 질문 처리 상태
public enum QnaStatus {
    //아직 답변이 등록되지 않은 상태
    OPEN,
    //관리자가 답변을 작성해 처리 완료된 상태
    ANSWERED
}
