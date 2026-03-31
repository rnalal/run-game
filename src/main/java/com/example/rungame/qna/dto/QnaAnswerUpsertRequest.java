package com.example.rungame.qna.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/*
* QnA 답변 등록/수정 요청 DTO
* - 관리자가 질문에 답변을 새로 작성하거나 수정할 때 요청 바디로 받는 데이터 모델
*
* - content 필드에 @NotBlank 적용 -> 빈 문자열,공백만 있는 답변은 허용하지 않도록 1차 방어
* - 답변에 필요한 최소 데이터만 담은 요청 DTO
* */
@Getter
public class QnaAnswerUpsertRequest {

    /*
    * 답변 내용
    *
    * - @NotBlank : 필수 입력 값
    * - 공백만 있는 경우도 막아서 최소한 한 글자 이상 입력하도록 강제
    * */
    @NotBlank
    private String content;
}
