package com.example.rungame.notice.repository;

import com.example.rungame.notice.domain.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    //대상+기간+제목으로 활성 공지 목록 조회
    List<Notice> findByTargetInAndPublishAtBeforeAndExpireAtAfterAndTitleContainingIgnoreCase(
            List<String> targets,
            LocalDateTime now1,
            LocalDateTime now2,
            String keyword
    );

    //관리자용 제목 검색+페이징
    Page<Notice> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);

    //공개용: 공지사항 목록 페이징 조회
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
