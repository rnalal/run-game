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

/*
* 관리자용 QnA 서비스
* - 관리자가 QnA 질문,답변을 조회하고 답변을 등록,수정,삭제할 수 있도록
*   도메인 로직을 담당하는 서비스 계층
*
* - 1)QnA 전체 목록 조회
*       - 검색어 기반 필터링
*       - 미답변 우선 정렬 로직은 레포지토리에서 처리
*       - 엔티티 -> 목록용 DTO 변환
* - 2)특정 질문 상세 조회
*       - 질문 + 답변까지 묶어서 QnaQuestionDetailDTO로 반환
* - 3)답변 등록/수정
*       - 답변이 없으면 새로 생성
*       - 이미 있으면 내용만 수정
*       - 질문 상태를 ANSWERED로 변경
* - 4)질문 삭제
*       - 질문 엔티티 삭제
*
* 트랜잭션
* - 클래스 레벨: @Transactional(readOnly=true)
*   -> 기본은 조회 전용
* - 변경이 필요한 메서드(upsertAnswer, deleteQuestion)는
*   -> 메서드 단위 @Transactional로 덮어서 쓰기 기능 트랜잭션으로 실행
* */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QnaAdminService {

    //QnA 질문 조회,검색,삭제 담당
    private final QnaQuestionRepository questionRepository;
    //QnA 답변 조회,저장 담당
    private final QnaAnswerRepository answerRepository;
    //작성자,관리자 이름 조회용
    private final UserRepository userRepository;

    /*
    * 관리자용 QnA 전체 목록 조회
    * - 미답변 먼저, 최신순으로 정렬된 질문 목록
    * - 제목/내용 기준 keyword 검색 지원
    *
    * @param keyword : 검색어
    * @param pageable : 페이징 정보
    * @return : QnaQuestionListItemDTO 페이지
    * */
    public Page<QnaQuestionListItemDTO> listAll(String keyword, Pageable pageable) {
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

    /*
    * 관리자용 질문 상세 조회
    * - 질문 1건 + 답변 1건까지 묶어서 상세 DTO로 변환
    * - 존재하지 않는 ID면 IllegalArgumentException 발생
    *
    * @param questionId : 조회할 질문 ID
    * @return : QnaQuestionDetailDTO
    * */
    public QnaQuestionDetailDTO getDetail(Long questionId) {
        QnaQuestion q = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("질문이 존재하지 않습니다."));
        return toDetailDto(q);
    }

    /*
    * 관리자 답변 등록,수정
    *
    * - 해당 질문이 존재하는지 먼저 검사
    * - 이미 답변이 있으면 -> 내용만 수정
    * - 답변이 없다면 -> 새로 생성
    * - 질문 상태를 ANSWERED로 변경
    * */
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

    /*
    * 질문 삭제(관리자)
    * - 질문이 없으면 예외 발생
    * - 실제 삭제 정책은 DB 제약조건 및 매핑 설정에 따라 동작
    *
    * @param adminId : 삭제를 수행하는 관리자 ID
    * @param questionID : 삭제할 질문 ID
    * */
    @Transactional
    public void deleteQuestion(Long adminId, Long questionId) {
        QnaQuestion q = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("질문이 존재하지 않습니다."));

        questionRepository.delete(q);
    }

    /*
    * 내부 헬퍼: QnA 질문 엔티티 -> 상세 DTO 변환
    *
    * - 질문 기본 정보
    * - 연결된 답변 정보
    *
    * - adminName 조회 시 현재는 userRepository를 사용하고 있으나
    *   실제 운영에서는 관리자 계정 테이블을 분리해서 사용하는 편이 더 자연스러움
    * */
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

}
