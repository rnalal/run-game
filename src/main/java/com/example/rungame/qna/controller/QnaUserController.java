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

/*
* 사용자용 QnA REST 컨트롤러
*
* - 로그인한 사용자가 QnA를 조회,작성,수정,삭제 할 수 있도록 해주는 API
*
* - 1)공개 질문 목록 조회
*       - 전체 공개된 QnA 목록
* - 2)내 질문 목록 조회
*       - 현재 로그인한 사용자가 작성한 질문만 페이징으로 조회
* - 3)질문 상세 조회
*       - 권한 체크 포함(내 질문인지, 공개 가능한 상태인지 등은 서비스에서 처리)
* - 4)질문 작성,수정,삭제
*       - 사용자 본인의 질문에 대해서만 변경 가능하도록 userId를 함께 넘겨 서비스에서 소유권 검증
* */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/qna")
public class QnaUserController {

    //사용자 QnA 도메인 로직을 담당하는 서비스
    private final QnaUserService qnaUserService;

    /*
    * 현재 로그인한 사용자 ID 반환
    * - SecurityContextHolder에서 Authentication을 꺼내서 principal을 Long userId로 캐스팅하는 메서드
    *
    * 전제
    * - JWT 필터에서 Authentication.principal에 Long 타입 userId를 넣어 둔 상태
    * */
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

    /*
    * 공개 QnA 목록 조회
    * - 로그인 여부와 상관없이 호출 가능
    * - 비밀글/권한 체크는 QnaUserService.listPublic 측에서 처리
    * */
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

    /*
    * 질문 상세 조회
    * - currentUserId()를 함께 넘겨서 비밀글이면 작성자 본인만 볼 수 있게
    *   공개 질문이면 다른 사람도 열람 가능하게 등의 기능을 서비스에서 구현
    * */
    @GetMapping("/questions/{id}")
    public ApiResponse<QnaQuestionDetailDTO> detail(@PathVariable Long id) {
        return ApiResponse.ok(
                qnaUserService.getQuestionDetail(currentUserId(), id)
        );
    }

    /*
    * 질문 작성
    * - currentUserId()로 작성자 ID를 가져와 어떤 유저가 이 질문을 만들었는지 함께 저장
    *
    * @return
    * - 생성된 질문의 ID
    *
    * 검증
    * - @Valid로 RequestBody에 대한 기본 Bean Validation 수행
    * */
    @PostMapping("/questions")
    public ApiResponse<Long> create(
            @Valid @RequestBody QnaQuestionCreateRequest req
    ) {
        return ApiResponse.ok(
                qnaUserService.createQuestion(currentUserId(), req)
        );
    }

    /*
    * 질문 수정
    * - currentUserId()와 질문 ID를 함께 넘겨서
    *   본인 질문만 수정 가능하게,이미 답변이 달린 질문은 수정 제한 두는 기능을 서비스에서 구현
    * */
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

    /*
    * 질문 여러 개 한 번에 삭제
    * - currentUserId(), ids 리스트를 넘겨 각 질문이 진짜 이 유저의 것인지
    *   확인한 뒤 일괄 삭제
    * */
    @DeleteMapping("/my/questions")
    public ApiResponse<?> deleteBulk(
            @RequestBody QnaQuestionBulkDeleteRequest req
    ){
        qnaUserService.deleteQuestionBulk(currentUserId(), req.getIds());
        return ApiResponse.ok(null);
    }
}
