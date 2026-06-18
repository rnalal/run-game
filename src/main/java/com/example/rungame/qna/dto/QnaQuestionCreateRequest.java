package com.example.rungame.qna.dto;

import com.example.rungame.qna.domain.QnaVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

//QnA 질문 생성 요청 DTO
@Getter
public class QnaQuestionCreateRequest {
    @NotBlank
    @Size(max=200)
    private String title;

    @NotBlank
    private String content;

    //질문 공개 범위
    @NotNull
    private QnaVisibility visibility;
}
