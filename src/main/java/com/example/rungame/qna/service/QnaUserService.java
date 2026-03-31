package com.example.rungame.qna.service;

import com.example.rungame.qna.domain.*;
import com.example.rungame.qna.dto.*;
import com.example.rungame.qna.exception.QnaAccessDeniedException;
import com.example.rungame.qna.repository.*;
import com.example.rungame.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/*
* 사용자용 QnA 서비스
* - 일반 사용자가 질문을 등록,조회,수정,삭제할 때의 전체 흐름을 담당하는 QnA 사용자 전용 서비스 레이어
*
* - 1)질문 작성
*       - 현재 로그인한 유저 ID + 작성 폼 DTO를 받아서 QnaQuestion 생성
* - 2)공개 질문 목록 조회
*       - visibility = PUBLIC 인 질문만
*       - keyword 검색 + 페이징
* - 3)내 질문 목록 조회
*       - userId 기준으로 내가 쓴 질문만
*       - 공개/비공개 모두 포함
* - 4)질문 상세 조회
*       - 비공개 질문이면
*           - 비로그인 -> LOGIN_REQUIRED
*           - 남의 질문 -> PRIVATE_QUESTION 예외 처리
*       - 그 외에는 질문 + 답변까지 DTO로 반환
* - 5)질문 수정,석제
*       - 내가 쓴 질문인지 검증
*       - 답변 완료된 질문은 수정,삭제 제한
*
* 트랜잭션
* - 클래스 전체는 readOnly = true
* - 데이터 변경이 필요한 메서드만 @Transactional로 별도 지정
* */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QnaUserService {

    private final QnaQuestionRepository questionRepository;
    private final QnaAnswerRepository answerRepository;
    private final UserRepository userRepository;

    /*
    * QnA 질문 생성
    *
    * @param userId : 현재 로그인한 사용자 ID
    * @param req : 제목,내용,공개범위가 담긴 생성 요청 DTO
    * @return : 생성된 질문 ID
    * */
    @Transactional
    public Long createQuestion(Long userId, QnaQuestionCreateRequest req) {
        QnaQuestion q = QnaQuestion.builder()
                .userId(userId)
                .title(req.getTitle())
                .content(req.getContent())
                .visibility(req.getVisibility())
                .build();
        return questionRepository.save(q).getId();
    }

    /*
    * 공개 질문 목록 조회
    * - visibility = PUBLIC 인 질문만 대상
    * - 제목,내용 keyword 검색 + 페이징
    * - 작성자 닉네임을 포함한 리스트 DTO로 변환
    * */
    public Page<QnaQuestionListItemDTO> listPublic(String keyword, Pageable pageable) {
        return questionRepository.searchPublic(keyword, pageable)
                .map(q -> {
                    String writerName = userRepository.findNicknameById(q.getUserId())
                            .orElse("알 수 없음");

                    return QnaQuestionListItemDTO.builder()
                            .id(q.getId())
                            .title(q.getTitle())
                            .visibility(q.getVisibility())
                            .status(q.getStatus())
                            .answered(q.getStatus() == QnaStatus.ANSWERED)
                            .writerName(writerName)
                            .createdAt(q.getCreatedAt())
                            .build();
                });
    }

    /*
    * 내 질문 목록 조회
    * - userId 기준으로 내가 작성한 질문들만
    * - 공개/비공개 상관없이 모두 포함
    * - 한 번 조회한 writerName 을 그대로 재사용해서 불필요한 DB 조회 줄이기
    * */
    public Page<QnaQuestionListItemDTO> listMine(Long userId, String keyword, Pageable pageable) {

        String writerName = userRepository
                .findNicknameById(userId)
                .orElse("나");

        return questionRepository.searchMine(userId, keyword, pageable)
                .map(q -> QnaQuestionListItemDTO.builder()
                        .id(q.getId())
                        .title(q.getTitle())
                        .visibility(q.getVisibility())
                        .status(q.getStatus())
                        .answered(q.getStatus() == QnaStatus.ANSWERED)
                        .writerName(writerName)
                        .createdAt(q.getCreatedAt())
                        .build());
    }

    /*
    * 질문 상세 조회
    *
    * 접근 제어 규칙
    * - PUBLIC 질문
    *   - 누구나 조회 가능
    * - PRIVATE 질문
    *   - 비로그인 -> QnaAccessDeniedException(LOGIN_REQUIRED)
    *   - 로그인했지만 작성자가 아님 -> QnaAccessDeniedException(PRIVATE_QUESTION)
    *
    * @param currentUserId : 현재 로그인한 사용자 ID
    * @param questionId : 조회할 질문 ID
    * */
    public QnaQuestionDetailDTO getQuestionDetail(Long currentUserId, Long questionId) {
        QnaQuestion q = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("질문이 존재하지 않습니다."));

        //비공개 질문 접근 제어
        if(q.getVisibility() == QnaVisibility.PRIVATE) {
            //로그인 안 한 경우
            if(currentUserId == null) {
                throw new QnaAccessDeniedException(
                        "LOGIN_REQUIRED",
                        "로그인이 필요합니다."
                );
            }
            //작성자가 아닌 경우
            if(!q.getUserId().equals(currentUserId)) {
                throw new QnaAccessDeniedException(
                        "PRIVATE_QUESTION",
                        "비공개 질문입니다."
                );
            }
        }

        return toDetailDto(q, currentUserId);
    }

    /*
    * 질문 수정
    * - 본인이 쓴 질문만 수정 가능
    * - 답변이 완료된 질문은 수정 불가
    * */
    @Transactional
    public void updateQuestion(Long currentUserId, Long questionId, QnaQuestionUpdateRequest req) {
        QnaQuestion q = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("질문이 존재하지 않습니다."));

        if (!q.getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("본인 질문만 수정할 수 있습니다.");
        }

        //답변 완료 후 수정 불가
        if (q.getStatus() == QnaStatus.ANSWERED) {
            throw new IllegalStateException("답변이 완료된 질문은 수정할 수 없습니다.");
        }

        q.update(req.getTitle(), req.getContent(), req.getVisibility());
    }

    /*
    * 질문 삭제
    * - 본인이 쓴 질문만 삭제 가능
    * - 답변이 완료된 질문은 삭제 불가
    * */
    @Transactional
    public void deleteQuestion(Long currentUserId, Long questionId) {
        QnaQuestion q = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("질문이 존재하지 않습니다."));
        if (!q.getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("본인 질문만 삭제할 수 있습니다.");
        }

        if(q.getStatus() == QnaStatus.ANSWERED) {
            throw new IllegalStateException("답변이 완료된 질문은 삭제할 수 없습니다.");
        }

        //답변이 있다면 FK / ON DELETE CASCADE 설정에 따라 함께 삭제
        questionRepository.delete(q);
    }

    /*
    * 질문 일괄 삭제
    * - 전달된 각 ID에 대해
    *   - 존재 여부 체크
    *   - 작성자 본인인지 체크
    *   - 답변 완료 여부 체크
    * - 하나라도 조건에 어긋나면 해당 예외를 바로 던짐
    * */
    @Transactional
    public void deleteQuestionBulk(Long userId, List<Long> ids) {
        for (Long id : ids) {
            QnaQuestion q = questionRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("질문이 존재하지 않습니다."));

            if(!q.getUserId().equals(userId)) {
                throw new IllegalArgumentException("본인 질문만 삭제할 수 있습니다.");
            }

            if(q.getStatus() == QnaStatus.ANSWERED) {
                throw new IllegalStateException("답변 완료된 질문은 삭제할 수 없습니다.");
            }

            questionRepository.delete(q);
        }
    }

    /*
    * 질문 엔티티를 사용자용 상세 DTO로 변환
    * - mine 플래그: 현재 사용자 기준으로 내 질문인지 여부
    * - 작성자 닉네임
    * - 연결된 답변이 있다면 QnaAnswerDTO로 포함
    * */
    private QnaQuestionDetailDTO toDetailDto(QnaQuestion q, Long currentUserId) {

        String writerName = userRepository
                .findNicknameById(q.getUserId())
                .orElse("알 수 없음");

        return QnaQuestionDetailDTO.builder()
                .id(q.getId())
                .userId(q.getUserId())
                .mine(q.getUserId().equals(currentUserId))
                .title(q.getTitle())
                .content(q.getContent())
                .writerName(writerName)
                .visibility(q.getVisibility())
                .status(q.getStatus())
                .createdAt(q.getCreatedAt())
                .updatedAt(q.getUpdatedAt())
                .answer(
                        answerRepository.findByQuestion_Id(q.getId())
                                .map(a -> QnaAnswerDTO.builder()
                                        .id(a.getId())
                                        .adminId(a.getAdminId())
                                        .content(a.getContent())
                                        .createdAt(a.getCreatedAt())
                                        .updatedAt(a.getUpdatedAt())
                                        .build())
                                .orElse(null)
                )
                .build();
    }
}


