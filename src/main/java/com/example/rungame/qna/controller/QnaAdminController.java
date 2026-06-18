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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rg-admin/qna")
public class QnaAdminController {

    private final QnaAdminService qnaAdminService;

    //전체 질문 목록 조회
    @GetMapping("/questions")
    public ApiResponse<Page<QnaQuestionListItemDTO>> listAll(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 4) Pageable pageable
    ) {
        return ApiResponse.ok(qnaAdminService.listAll(keyword, pageable));
    }

    //질문 상제 조회
    @GetMapping("/questions/{id}")
    public ApiResponse<QnaQuestionDetailDTO> detail(@PathVariable Long id) {
        return ApiResponse.ok(qnaAdminService.getDetail(id));
    }

    //답변 등록,수정
    @PutMapping("/questions/{id}/answer")
    public ApiResponse<?> upsertAnswer(
            @PathVariable Long id,
            @Valid @RequestBody QnaAnswerUpsertRequest req
    ) {
        Long adminId = AdminSecurityUtil.getCurrentAdminId();
        qnaAdminService.upsertAnswer(adminId, id, req);
        return ApiResponse.ok(null);
    }

    //질문 삭제
    @DeleteMapping("/questions/{id}")
    public ApiResponse<?> deleteQuestion(@PathVariable Long id){
        Long adminId = AdminSecurityUtil.getCurrentAdminId();
        qnaAdminService.deleteQuestion(adminId, id);
        return ApiResponse.ok(null);
    }
}
