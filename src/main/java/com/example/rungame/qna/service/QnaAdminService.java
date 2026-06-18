package com.example.rungame.qna.service;

import com.example.rungame.qna.domain.*;
import com.example.rungame.qna.dto.*;
import com.example.rungame.qna.repository.*;
import com.example.rungame.user.domain.User;
import com.example.rungame.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QnaAdminService {

    private final QnaQuestionRepository questionRepository;
    private final QnaAnswerRepository answerRepository;
    private final UserRepository userRepository;

    //관리자용 QnA 전체 목록 조회
    public Page<QnaQuestionListItemDTO> listAll(String keyword, Pageable pageable) {
        return questionRepository.searchAllAdminListItems(keyword, pageable);
    }

    //관리자용 질문 상세 조회
    public QnaQuestionDetailDTO getDetail(Long questionId) {
        QnaQuestion q = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("질문이 존재하지 않습니다."));
        return toDetailDto(q);
    }

    //관리자 답변 등록,수정
    @Transactional
    public void upsertAnswer(Long adminId, Long questionId, QnaAnswerUpsertRequest req) {
        QnaQuestion q = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("질문이 존재하지 않습니다."));

        QnaAnswer answer = answerRepository.findByQuestion_Id(questionId)
                .orElse(null);

        if (answer == null) {
            //최초 답변 등록
            answerRepository.save(QnaAnswer.builder()
                    .question(q)
                    .adminId(adminId)
                    .content(req.getContent())
                    .build());
        } else {
            //기존 답변 내용 수정
            answer.updateContent(req.getContent());
        }
        //질문 상태를 ANSWERED로 변경
        q.markAnswered();
    }

    //질문 삭제(관리자)
    @Transactional
    public void deleteQuestion(Long adminId, Long questionId) {
        QnaQuestion q = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("질문이 존재하지 않습니다."));

        questionRepository.delete(q);
    }

    //내부 헬퍼: QnA 질문 엔티티 -> 상세 DTO 변환
    private QnaQuestionDetailDTO toDetailDto(QnaQuestion q) {

        String writerName = userRepository.findById(q.getUserId())
                .map(User::getNickname)
                .orElse("탈퇴한 사용자");

        QnaAnswerDTO answerDto = answerRepository.findByQuestion_Id(q.getId())
                .map(a -> {

                    String adminName = userRepository.findById(q.getUserId())
                            .map(User::getNickname)
                            .orElse("알 수 없는 관리자");

                    return QnaAnswerDTO.builder()
                            .id(a.getId())
                            .adminId(a.getAdminId())
                            .adminName(adminName)
                            .content(a.getContent())
                            .createdAt(a.getCreatedAt())
                            .updatedAt(a.getUpdatedAt())
                            .build();
                })
                .orElse(null);

        return QnaQuestionDetailDTO.builder()
                .id(q.getId())
                .userId(q.getUserId())
                .writerName(writerName)
                .title(q.getTitle())
                .content(q.getContent())
                .visibility(q.getVisibility())
                .status(q.getStatus())
                .createdAt(q.getCreatedAt())
                .updatedAt(q.getUpdatedAt())
                .answer(answerDto)
                .build();
    }

    public Page<QnaQuestionListItemDTO> listAllBefore(String keyword, Pageable pageable) {
        return questionRepository.searchAllAdmin(keyword, pageable)
                .map(q -> {

                    String writerName = userRepository.findById(q.getUserId())
                            .map(User::getNickname)
                            .orElse("탈퇴한 사용자");

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
}
