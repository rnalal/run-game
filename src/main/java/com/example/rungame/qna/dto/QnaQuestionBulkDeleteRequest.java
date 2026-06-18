package com.example.rungame.qna.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

import java.util.List;

//QnA 질문 일괄 삭제 요청 DTO
@Getter
public class QnaQuestionBulkDeleteRequest {

    //일괄 삭제할 질문 ID 목록
    @NotEmpty
    private List<Long> ids;

}
