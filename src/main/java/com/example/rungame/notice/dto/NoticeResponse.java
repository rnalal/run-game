package com.example.rungame.notice.dto;

import com.example.rungame.notice.domain.Notice;
import lombok.Getter;

/*
* 공지사항 응답용 DTO
* - Notice 엔티티를 그대로 노출하지 않고
*   클라이언트에 맞는 형태로 가공해서 전달하는 응답 전용 모델
* */
@Getter
public class NoticeResponse {

    private Long id;
    private String title;
    private String content;
    private boolean popup;
    private String target;
    /*
    * 게시 시작 시각
    * - 엔티티의 LocalDateTime -> String으로 변환
    * */
    private String publishAt;
    //게시 종료 시각
    private String expireAt;
    //공지 생성 시각
    private String createdAt;
    //마지막 수정 시각
    private String updatedAt;

    /*
    * Notice 엔티티를 기반으로 응답 DTO 생성
    * - 도메인 모델을 그대로 외부에 노출하지 않고 필요한 필드만 뽑아서
    *   날짜 타입은 String 으로 변환
    * - null 체크를 통해 값이 없으면 그대로 null 유지
    * */
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
