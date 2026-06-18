package com.example.rungame.qna.dto;

import com.example.rungame.qna.domain.QnaStatus;
import com.example.rungame.qna.domain.QnaVisibility;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

//QnA 질문 상세 조회용 DTO
@Getter
@Builder
public class QnaQuestionDetailDTO {
    private Long id;
    private Long userId;

    //현재 조회 중인 사용자가 이 질문의 작성자인지 여부
    private boolean mine;
    private String title;
    private String content;
    private String writerName;
    private QnaVisibility visibility;
    private QnaStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    //답변 정보
    private QnaAnswerDTO answer;
}
