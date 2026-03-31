package com.example.rungame.qna.controller;

import com.example.rungame.admin.security.AdminSecurityUtil;
import com.example.rungame.common.dto.ApiResponse;
import com.example.rungame.qna.dto.*;
import com.example.rungame.qna.service.QnaAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

/*
* 관리자용 QnA 관리 REST 컨트롤러
* - 관리자가 유저 질문을 조회하고 답변을 등록,수정,삭제할 수 있게 해주는 API
* */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rg-admin/qna")
public class QnaAdminController {

    //관리자 QnA 도메인 로직을 담당하는 서비스
    private final QnaAdminService qnaAdminService;

    /*
    * 전체 질문 목록 조회
    *
    * 응답
    * - ApiResponse<Page<QnaQuestionListItemDTO>> : 한 페이지 분량의 질문 묵록 + 페이징 메타 정보
    * */
    @GetMapping("/questions")
    public ApiResponse<Page<QnaQuestionListItemDTO>> listAll(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 4) Pageable pageable
    ) {
        return ApiResponse.ok(qnaAdminService.listAll(keyword, pageable));
    }

    /*
    * 질문 상제 조회
    *
    * @param id : 상제 확인할 질문 ID
    * @return : 질문 기본 정보 + 작성자 정보 + 답변 내용 등이 담긴 DTO
    * */
    @GetMapping("/questions/{id}")
    public ApiResponse<QnaQuestionDetailDTO> detail(@PathVariable Long id) {
        return ApiResponse.ok(qnaAdminService.getDetail(id));
    }

    /*
    * 답변 등록,수정
    *
    * - 현재 로그인한 관리자 ID를 AdminSecurityUtil에서 가져와 누가 답변을 작성,수정했는지 함께 넘김
    * - 질문에 답변이 없으면 등록, 이미 있으면 수정 하는 형태의 패턴
    *
    * 응답
    * - ApiResponse.ok(null) : 바디는 사용하지 않고 성공 여부만 전달
    * */
    @PutMapping("/questions/{id}/answer")
    public ApiResponse<?> upsertAnswer(
            @PathVariable Long id,
            @Valid @RequestBody QnaAnswerUpsertRequest req
    ) {
        Long adminId = AdminSecurityUtil.getCurrentAdminId();
        qnaAdminService.upsertAnswer(adminId, id, req);
        return ApiResponse.ok(null);
    }

    /*
    * 질문 삭제
    * - 현재 관리자 ID와 함께 서비스로 전달해서 삭제 권한 체크나 감사 로그 등에 활용 가능
    *
    * 응답
    * - ApiResponse.ok(null) : 성공 여부만 반환
    * */
    @DeleteMapping("/questions/{id}")
    public ApiResponse<?> deleteQuestion(@PathVariable Long id){
        Long adminId = AdminSecurityUtil.getCurrentAdminId();
        qnaAdminService.deleteQuestion(adminId, id);
        return ApiResponse.ok(null);
    }
}
