package com.example.rungame.qna.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

//QnA 답변 조회용 DTO
@Getter
@Builder
public class QnaAnswerDTO {
    private Long id;
    private Long adminId;
    private String adminName;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
