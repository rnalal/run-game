package com.example.rungame.qna.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

import java.util.List;

/*
* QnA 질문 일괄 삭제 요청 DTO
* - 사용자가 내 질문 목록에서 여러 개를 한 번에 선택해서 삭제할 때
*   삭제 대상 질문 ID 목록을 서버로 전달하기 위한 요청 모델
* */
@Getter
public class QnaQuestionBulkDeleteRequest {

    /*
    * 일괄 삭제할 질문 ID 목록
    * - @NotEmpty : 비어 있으면 안됨
    * - 컨트롤러에서 currentUserId()와 함께 넘겨서 각 ID가 현재 로그인한 사용자의 질문인지를
    *   서비스에서 검증 후 삭제
    * */
    @NotEmpty
    private List<Long> ids;

}
