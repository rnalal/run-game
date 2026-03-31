package com.example.rungame.notice.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
* 공지사항 도메인 엔티티
* - 관리자 공지 등록,수정,삭제, 클라이언트 노출의 기본이 되는 엔티티
* - 팝업 여부, 노출 기간, 노출 대상 같은 메타 정보를 함께 관리
* */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "notices")
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    //@Lob : 길이 제한 없이 저장
    @Lob
    private String content;

    //팝업 공지 여부
    private boolean popup;

    /*
    * 공지 게시 시작 시각
    * - PublicNoticeController에서 now 기준으로 publishAt 이후인지 체크
    * */
    private LocalDateTime publishAt;

    /*
    * 공지 게시 종료 시각
    * - 이 시각이 지나면 더 이상 공지를 노출하지 않음
    * - 공지 만료 처리 용도
    * */
    private LocalDateTime expireAt;

    /*
    * 공지 노출 대상
    * - PublicNoticeController에서 로그인한 사용자가 관리자냐 아니냐에 따라
    *   포함할 target 목록을 다르게 가져감
    * */
    private String target;

    /*
    * 공지 생성 시각
    * - @PrePersist에서 자동 설정
    * - 최초 등록 시점 기록
    * */
    private LocalDateTime createdAt;

    /*
    * 마지막 수정 시각
    * - @PrePersist, @PreUpdate 에서 자동 갱신
    * - 관리자 페이지에서 최종 수정일 표시 등에 사용
    * */
    private LocalDateTime updatedAt;

    //최초 저장 시 createdAt/updatedAt 자동 설정
    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    //업데이트 시 updatedAt 자동 갱신
    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
