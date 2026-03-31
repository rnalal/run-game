package com.example.rungame.qna.domain;

/*
* QnA 질문 처리 상태
* - 질문이 현재 답변 대기 인지, 답변 완료 상태인지 나타내는 Enum
* - 문자열 대신 타입으로 상태를 강제해서 오타나 잘못된 값이 저장되는 걸 막음
* */
public enum QnaStatus {
    //아직 답변이 등록되지 않은 상태
    OPEN,
    //관리자가 답변을 작성해 처리 완료된 상태
    ANSWERED
}
