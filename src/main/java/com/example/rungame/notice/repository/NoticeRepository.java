package com.example.rungame.notice.repository;

import com.example.rungame.notice.domain.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/*
* 공지사항 조회용 JPA 레포지토리
* - 공지 도메인에 대한 다양한 조회 쿼리를 한 곳에 모다 둔 인터페이스
*
* - 1)대상 + 기간 + 제목 조건으로 활성 공지 조회
* - 2)관리자 화면용 검색 + 페이징
* - 3)클라이언트 공개용 공지 목록 페이징
* - 4)현재 시점에 노출 가능한 팝업 공지 조회
* */
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    /*
    * 대상+기간+제목으로 활성 공지 목록 조회
    * - targest: 노출 대상 목록
    * - now1/now2: 현재 시각 기준
    *       - publishAtBefore(now1): 이미 게시가 시작된 공지만
    *       - expiredAtAfter(now2): 아직 만료되지 않은 공지만
    * - keyword: 제목 검색 키워드
    * */
    List<Notice> findByTargetInAndPublishAtBeforeAndExpireAtAfterAndTitleContainingIgnoreCase(
            List<String> targets,
            LocalDateTime now1,
            LocalDateTime now2,
            String keyword
    );

    /*
    * 관리자용 제목 검색+페이징
    * - 간단한 제목 검색만 필요한 경우에 사용
    * */
    Page<Notice> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);

    /*
    * 공개용: 공지사항 목록 페이징 조회
    *
    * 필터 조건
    * - n.target IN :targets
    * - 제목 조건 : title이 null이 아니면 LIKE 검색
    * - 게시 기간 :
    *       - publishAt 이 null 이거나 현재 시각 이전
    *       - expireAt 이 null 이거나 현재 시각 이후
    * */
    @Query("""
                SELECT n FROM Notice n
                WHERE n.target IN :targets
                    AND (:title IS NULL OR n.title LIKE %:title%)
                    AND (n.publishAt IS NULL OR n.publishAt <= CURRENT_TIMESTAMP)
                    AND (n.expireAt IS NULL OR n.expireAt >= CURRENT_TIMESTAMP)
                ORDER BY n.createdAt DESC
            """)
    Page<Notice> searchPublish(
            List<String> targets,
            String title,
            Pageable pageable
    );

    //현재 노출 가능한 팝업 공지 목록 조회
    @Query("""
        SELECT n FROM Notice n
        WHERE n.popup = true
        AND (n.publishAt IS NULL OR n.publishAt <= CURRENT_TIMESTAMP)
        AND (n.expireAt IS NULL OR n.expireAt >= CURRENT_TIMESTAMP)
        ORDER BY n.createdAt DESC
        """)
    List<Notice> findActivePopups();

    //관리자용 공지 검색 + 페이징
    @Query("""
        SELECT n FROM Notice n
        WHERE (:keyword IS NULL OR LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:target IS NULL OR n.target = :target)
          AND (:popup IS NULL OR n.popup = :popup)
        ORDER BY n.createdAt DESC
        """)
    Page<Notice> searchAdmin(
        @Param("keyword") String keyword,
        @Param("target") String target,
        @Param("popup") Boolean popup,
        Pageable pageable
    );

}
