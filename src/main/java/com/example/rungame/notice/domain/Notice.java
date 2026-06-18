package com.example.rungame.notice.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    //공지 게시 시작 시각
    private LocalDateTime publishAt;

    //공지 게시 종료 시각
    private LocalDateTime expireAt;

    //공지 노출 대상
    private String target;

    //공지 생성 시각
    private LocalDateTime createdAt;

    //마지막 수정 시각
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
