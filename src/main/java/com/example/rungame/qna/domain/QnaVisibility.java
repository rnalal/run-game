package com.example.rungame.qna.domain;

/*
* QnA 질문 공개 범위
* - 질문이 전체에게 보이는지 비밀글인지 표현하는 상태 값
* */
public enum QnaVisibility {
    //전체 공개 질문
    //다른 유저들도 목록,상세 열람 가능
    PUBLIC,

    //비밀글
    //작성자 본인 + 관리자만 볼 수 있도록 제한
    PRIVATE
}
