package com.example.rungame.notice.dto;

import com.example.rungame.notice.domain.Notice;
import lombok.Getter;

@Getter
public class NoticeResponse {

    private Long id;
    private String title;
    private String content;
    private boolean popup;
    private String target;
    //게시 시작 시각
    private String publishAt;
    //게시 종료 시각
    private String expireAt;
    //공지 생성 시각
    private String createdAt;
    //마지막 수정 시각
    private String updatedAt;

    //Notice 엔티티를 기반으로 응답 DTO 생성
    public NoticeResponse(Notice n) {
        this.id = n.getId();
        this.title = n.getTitle();
        this.content = n.getContent();
        this.popup = n.isPopup();
        this.target = n.getTarget();

        this.publishAt = n.getPublishAt() == null ? null : n.getPublishAt().toString();
        this.expireAt = n.getExpireAt() == null ? null : n.getExpireAt().toString();
        this.createdAt = n.getCreatedAt() == null ? null : n.getCreatedAt().toString();
        this.updatedAt = n.getUpdatedAt() == null ? null : n.getUpdatedAt().toString();
    }
}
