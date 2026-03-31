package com.example.rungame.qna.dto;

import com.example.rungame.qna.domain.QnaVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/*
* QnA 질문 생성 요청 DTO
* - 사용자가 새 QnA 질문을 작성할 때 제목,내용,공개 범위를 서버로 전달하기 위한 요청 모델
* */
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
