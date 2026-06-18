package com.example.rungame.qna.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

//QnA 답변 등록/수정 요청 DTO
@Getter
public class QnaAnswerUpsertRequest {

    //답변 내용
    @NotBlank
    private String content;
}
