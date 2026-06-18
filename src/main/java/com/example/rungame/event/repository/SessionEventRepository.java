package com.example.rungame.event.repository;

import com.example.rungame.event.domain.EventType;
import com.example.rungame.event.domain.SessionEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SessionEventRepository extends JpaRepository<SessionEvent, Long>,
        JpaSpecificationExecutor<SessionEvent> {

    interface EventTypeCount {
        EventType getType();
        Long getEventCount();
    }

    //특정 세션의 모든 이벤트를 seq 오름차순으로 조회
    List<SessionEvent> findBySessionIdOrderBySeqAsc(Long sessionId);
    //특정 세션에서 가장 마지막 이벤트 조회
    Optional<SessionEvent> findTopBySessionIdOrderBySeqDesc(Long sessionId);

    //특정 세션의 최근 이벤트 목록 조회
    @Query("""
            select e 
            from SessionEvent e
            where e.sessionId = :sessionId
            order by e.tMs desc, e.id desc
            """)
    List<SessionEvent> findRecentBySessionIdOrderByTmsDescIdDesc(
            @Param("sessionId") Long sessoinId,
            Pageable pageable
    );

    //세션 내 특정 타입의 이벤트 존재 여부 체크
    boolean existsBySessionIdAndType(Long sessionId, EventType type);

    //세션 내 특정 타입 이벤트를 최근 순으로 모두 조회
    @Query("""
            select e from SessionEvent e
            where e.sessionId = :sid and e.type = :type
            order by e.tMs desc, e.id desc
            """)
    List<SessionEvent> findAllBySessionIdAndTypeOrderByTmsDescIdDesc(
            @Param("sid") Long sessionId,
            @Param("type") EventType type
    );

    //세션 내에서 특정 타입의 마지막 이벤트 한 건만 가져오는 헬퍼
    default Optional<SessionEvent> findLastBySessionIdAndType(Long sessionId, EventType type){
        var list = findAllBySessionIdAndTypeOrderByTmsDescIdDesc(sessionId, type);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    //세션 내 특정 타입 이벤트 누적 개수 카운트
    long countBySessionIdAndType(Long sessionId, EventType type);

    //유저 ID를 기준으로 최근 이벤트 50개 조회
    @Query("""
                select e 
                from SessionEvent e
                join com.example.rungame.session.domain.Session s ON e.sessionId = s.id
                where s.userId = :userId
                order by e.createdAt desc
            """)
    List<SessionEvent> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);

    //특정 유저가 발생시킨 전체 이벤트 수
    @Query("""
                select count(e)
                from SessionEvent e
                join com.example.rungame.session.domain.Session s on e.sessionId = s.id
                where s.userId = :userId
            """)
    long countByUserId(Long userId);

    //특정 타입 이벤트가 특정 시각 이후에 몇 번 발생했는지 카운트
    long countByTypeAndCreatedAtAfter(EventType type, java.time.LocalDateTime after);

    //이벤트 검색
    @Query("""
        select e from SessionEvent e
        join com.example.rungame.session.domain.Session s on e.sessionId = s.id
        where (:type is null or e.type = :type)
          and (:userId is null or s.userId = :userId)
          and (:sessionId is null or e.sessionId = :sessionId)
          and (:fromAt is null or e.createdAt >= :fromAt)
          and (:toAt is null or e.createdAt < :toAt)
        order by e.createdAt desc, e.id desc
    """)
    Page<SessionEvent> searchPage(
            @Param("type") com.example.rungame.event.domain.EventType type,
            @Param("userId") Long userId,
            @Param("sessionId") Long sessionId,
            @Param("fromAt") LocalDateTime fromAt,
            @Param("toAt") LocalDateTime toAt,
            Pageable pageable
    );

    //이벤트 검색(전체 리스트 반환, 페이징 없음)
    @Query("""
        select e from SessionEvent e
        join com.example.rungame.session.domain.Session s on e.sessionId = s.id
        where (:type is null or e.type = :type)
          and (:userId is null or s.userId = :userId)
          and (:sessionId is null or e.sessionId = :sessionId)
          and (:fromAt is null or e.createdAt >= :fromAt)
          and (:toAt is null or e.createdAt < :toAt)
        order by e.createdAt desc, e.id desc
    """)
    List<SessionEvent> searchAll(
            @Param("type") com.example.rungame.event.domain.EventType type,
            @Param("userId") Long userId,
            @Param("sessionId") Long sessionId,
            @Param("fromAt") LocalDateTime fromAt,
            @Param("toAt") LocalDateTime toAt
    );

    //전체 이벤트 중 특정 타입의 누적 개수
    long countByType(EventType type);

    //관리자 대시보드 이벤트 TOP5 차트용
    @Query("""
            select e.type as type,
                count(e) as eventCount
            from SessionEvent e
            group by e.type
            """)
    List<EventTypeCount> countGroupByType();
}
