package com.example.rungame.qna.dto;

import com.example.rungame.qna.domain.QnaStatus;
import com.example.rungame.qna.domain.QnaVisibility;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/*
* QnA 질문 상세 조회용 DTO
* - 질문 1건에 대한 상세 정보 + 답변 정보까지 화면에서 바로 사용할 수 있는
*   형태로 묶어 전달하는 응답 모델
* */
@Getter
@Builder
public class QnaQuestionDetailDTO {
    private Long id;
    private Long userId;

    /*
    * 현재 조회 중인 사용자가 이 질문의 작성자인지 여부
    * - true: 내 질문
    * - false: 다른 사람이 쓴 질문
    *
    * - 수정,삭제 버튼 노출 여부를 쉽게 판단하기 위한 플래그
    * */
    private boolean mine;
    private String title;
    private String content;
    private String writerName;
    private QnaVisibility visibility;
    private QnaStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /*
    * 답변 정보
    * - 답변이 존재하면 QnaAnswerDTO
    * - 아직 답변이 없으면 null
    * */
    private QnaAnswerDTO answer;
}
