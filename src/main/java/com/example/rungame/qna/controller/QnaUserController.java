package com.example.rungame.qna.controller;

import com.example.rungame.common.dto.ApiResponse;
import com.example.rungame.qna.dto.*;
import com.example.rungame.qna.service.QnaUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/qna")
public class QnaUserController {

    private final QnaUserService qnaUserService;

    //현재 로그인한 사용자 ID 반환
    private Long currentUserId() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
            authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof Long userId) {
            return userId;
        }

        throw new IllegalStateException("인증 사용자 ID를 가져올 수 없습니다.");
    }

    //공개 QnA 목록 조회
    @GetMapping("/questions")
    public ApiResponse<Page<QnaQuestionListItemDTO>> listPublic(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 4, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ApiResponse.ok(
                qnaUserService.listPublic(keyword, pageable)
        );
    }

    //내 질문 목록 조회
    @GetMapping("/my/questions")
    public ApiResponse<Page<QnaQuestionListItemDTO>> listMine(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 4, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ApiResponse.ok(
                qnaUserService.listMine(currentUserId(), keyword, pageable)
        );
    }

    //질문 상세 조회
    @GetMapping("/questions/{id}")
    public ApiResponse<QnaQuestionDetailDTO> detail(@PathVariable Long id) {
        return ApiResponse.ok(
                qnaUserService.getQuestionDetail(currentUserId(), id)
        );
    }

    //질문 작성
    @PostMapping("/questions")
    public ApiResponse<Long> create(
            @Valid @RequestBody QnaQuestionCreateRequest req
    ) {
        return ApiResponse.ok(
                qnaUserService.createQuestion(currentUserId(), req)
        );
    }

    //질문 수정
    @PutMapping("/questions/{id}")
    public ApiResponse<?> update(
            @PathVariable Long id,
            @Valid @RequestBody QnaQuestionUpdateRequest req
    ) {
        qnaUserService.updateQuestion(currentUserId(), id, req);
        return ApiResponse.ok(null);
    }

    //질문 단건 삭제
    @DeleteMapping("/questions/{id}")
    public ApiResponse<?> delete(@PathVariable Long id) {
        qnaUserService.deleteQuestion(currentUserId(), id);
        return ApiResponse.ok(null);
    }

    //질문 여러 개 한 번에 삭제
    @DeleteMapping("/my/questions")
    public ApiResponse<?> deleteBulk(
            @RequestBody QnaQuestionBulkDeleteRequest req
    ){
        qnaUserService.deleteQuestionBulk(currentUserId(), req.getIds());
        return ApiResponse.ok(null);
    }
}
