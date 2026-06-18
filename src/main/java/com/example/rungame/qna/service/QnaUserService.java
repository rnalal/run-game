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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QnaUserService {

    private final QnaQuestionRepository questionRepository;
    private final QnaAnswerRepository answerRepository;
    private final UserRepository userRepository;

    //QnA 질문 생성
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

    //공개 질문 목록 조회
    public Page<QnaQuestionListItemDTO> listPublic(String keyword, Pageable pageable) {
        return questionRepository.searchPublicListItems(keyword, pageable);
    }

    //내 질문 목록 조회
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

    //질문 상세 조회
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

    //질문 수정
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

    //질문 삭제
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

    //질문 일괄 삭제
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

    //질문 엔티티를 사용자용 상세 DTO로 변환
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


