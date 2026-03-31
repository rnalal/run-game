package com.example.rungame.qna.dto;

import com.example.rungame.qna.domain.QnaStatus;
import com.example.rungame.qna.domain.QnaVisibility;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/*
* QnA 질문 목록용 DTO
* - QnA 리스트 화면에 뿌려질 질문 한 줄 정보를 담는 가벼운 응답 모델
* */
@Getter
@Builder
public class QnaQuestionListItemDTO {
    private Long id;
    private String title;
    //질문 공개 범위
    private QnaVisibility visibility;
    //질문 상태
    private QnaStatus status;
    /*
    * 답변 여부 편의 플래그
    * - true: 답변이 존재
    * - false: 아직 답변이 없음
    * */
    private boolean answered;
    private String writerName;
    private LocalDateTime createdAt;
}
