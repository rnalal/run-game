package com.example.rungame.qna.repository;

import com.example.rungame.qna.domain.QnaAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/*
* QnA 답변 전용 레포지토리
* - QnaAnswer 엔티티에 대한 기본 CRUD + 질문 기준 조회를 담당하는 저장소 인터페이스
* */
public interface QnaAnswerRepository extends JpaRepository<QnaAnswer, Long> {

    /*
    * 특정 질문에 대한 답변 조회
    *
    * @param questionId : 질문 ID
    * @return : 해당 질문에 달린 답변
    * */
    Optional<QnaAnswer> findByQuestion_Id(Long questionId);

    /*
    * 특정 질문에 이미 답변이 존재하는지 여부 확인
    *
    * @param questionId : 질문 ID
    * @return true : 답변이 이미 있음
    *         false : 아직 답변이 없음
    * */
    boolean existsByQuestion_Id(Long questionId);
}
