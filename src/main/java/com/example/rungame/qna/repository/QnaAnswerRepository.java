package com.example.rungame.qna.repository;

import com.example.rungame.qna.domain.QnaAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

//QnA 답변 전용 레포지토리
public interface QnaAnswerRepository extends JpaRepository<QnaAnswer, Long> {

    //특정 질문에 대한 답변 조회
    Optional<QnaAnswer> findByQuestion_Id(Long questionId);

    //특정 질문에 이미 답변이 존재하는지 여부 확인
    boolean existsByQuestion_Id(Long questionId);
}
