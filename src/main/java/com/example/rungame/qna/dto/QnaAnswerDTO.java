package com.example.rungame.qna.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/*
* QnA 답변 조회용 DTO
* - QnaAnswer 엔티티 + 관리자 이름까지 합쳐서 화면/클라이언트에서 바로 쓰기 좋은 형태로
*   만든 답변 응답 전용 모델
* */
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
