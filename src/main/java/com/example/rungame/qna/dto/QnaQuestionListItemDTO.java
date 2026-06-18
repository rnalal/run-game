package com.example.rungame.qna.dto;

import com.example.rungame.qna.domain.QnaStatus;
import com.example.rungame.qna.domain.QnaVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

//QnA 질문 목록용 DTO
@Getter
@Builder
@AllArgsConstructor
public class QnaQuestionListItemDTO {
    private Long id;
    private String title;
    private QnaVisibility visibility;
    private QnaStatus status;
    private boolean answered;
    private String writerName;
    private LocalDateTime createdAt;
}
