package com.example.rungame.qna.repository;

import com.example.rungame.qna.domain.QnaQuestion;
import com.example.rungame.qna.domain.QnaVisibility;
import com.example.rungame.qna.domain.QnaStatus;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

/*
* QnA 질문 JPA 레포지토리
* - QnaQuestion 엔티티에 대한 기본 CRUD + 목록 조회,검색 패턴을 사용자/관리자 용도로 나눠서
*   제공하는 저장소 인터페이스
* */
public interface QnaQuestionRepository extends JpaRepository<QnaQuestion, Long> {

    /*
    * 사용자: 공개 질문 목록 + 검색
    * - visibility = PUBLIC 인 질문만
    *
    * - keyword가 null이거나 빈 문자열이면 전체 조회
    * - 그렇지 않으면
    *       - title LIKE %keyword%
    *       - OR content LIKE %keyword%
    *
    * - 정렬 기준은 Pageable에서 지정
    * */
    @Query("""
          select q from QnaQuestion q
          where q.visibility = 'PUBLIC'
            and (
              :keyword is null or :keyword = ''
              or lower(q.title) like lower(concat('%', :keyword, '%'))
              or q.content like concat('%', :keyword, '%')
            )
    """)
    Page<QnaQuestion> searchPublic(
            @Param("keyword") String keyword,
            Pageable pageable
    );


    /*
    * 사용자: 내 질문 공개, 비공개 목록 + 검색
    * - q.userId = :userId 인 질문만
    * - visibility는 PUBLIC/PRIVATE 모두 포함
    *
    * - keyword null/빈 문자열 -> 검색 없이 전체
    * - 그 외 -> 제목,내용에 keyword 포함 여부로 필터
    * */
    @Query("""
          select q from QnaQuestion q
          where q.userId = :userId
            and (
              :keyword is null or :keyword = ''
              or lower(q.title) like lower(concat('%', :keyword, '%'))
              or q.content like concat('%', :keyword, '%')
            )
    """)
    Page<QnaQuestion> searchMine(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    /*
    * 관리자: 전체 질문 목록 + 검색 + 미답변OPEN 먼저, 최신순 정렬
    * - 모든 질문(PUBLIC/PRIVATE 구분 없이)
    *
    * - keyword null/빈 문자열 -> 전체
    * - 그 외 -> 제목/내용 기준 검색
    *
    * order by
    * - 1순위: status = OPEN 인 질문 먼저
    * - 2순위: createdAt desc (최신 질문부터)
    * */
    @Query("""
          select q from QnaQuestion q
          where (
            :keyword is null or :keyword = ''
            or lower(q.title) like lower(concat('%', :keyword, '%'))
            or q.content like concat('%', :keyword, '%')
          )
          order by 
            case when q.status = 'OPEN' then 0 else 1 end,
            q.createdAt desc
    """)
    Page<QnaQuestion> searchAllAdmin(
            @Param("keyword") String keyword,
            Pageable pageable
    );

}
