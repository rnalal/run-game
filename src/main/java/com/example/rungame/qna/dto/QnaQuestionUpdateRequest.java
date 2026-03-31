package com.example.rungame.qna.dto;

import com.example.rungame.qna.domain.QnaVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/*
* QnA 질문 수정 요청 DTO
* - 사용자가 이미 작성한 QnA 질문의 제목,내용,공개 범위를 수정할 때 사용하는 요청 바디 모델
* */
@Getter
public class QnaQuestionUpdateRequest {
    @NotBlank
    @Size(max=200)
    private String title;

    @NotBlank
    private String content;

    //@NotNull : null 허용 X, 수정 시에도 반드시 명시적으로 선택하도록 강제
    @NotNull
    private QnaVisibility visibility;
}
