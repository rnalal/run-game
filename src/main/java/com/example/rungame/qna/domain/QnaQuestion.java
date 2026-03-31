package com.example.rungame.qna.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/*
* QnA 질문 엔티티
* - 사용자가 남긴 1:1 문의 질문을 저장하고 공개 범위와 답변 상태를 함께 관리하는 도메인 모델
* */
@Entity
@Table(name = "qna_question",
        indexes = {
                @Index(name="idx_qna_question_visibility_created", columnList="visibility, created_at"),
                @Index(name="idx_qna_question_user_created", columnList="user_id, created_at"),
                @Index(name="idx_qna_question_status_created", columnList="status, created_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QnaQuestion {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //질문 작성자
    @Column(name="user_id", nullable=false)
    private Long userId;

    @Column(length=200, nullable=false)
    private String title;

    /*
    * 질문 본문 내용
    * - @Lob 으로 길이 제한 없이 저장
    * */
    @Lob
    @Column(nullable=false)
    private String content;

    //질문 공개 범위
    //기본값 - QnaVisibility.PUBLIC
    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private QnaVisibility visibility = QnaVisibility.PUBLIC;

    //질문 처리 상태
    //기본값 - QnaStatus.OPEN
    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private QnaStatus status = QnaStatus.OPEN;

    /*
    * 질문 생성 시각
    * - 최초 저장 시점에만 설정
    * */
    @Column(name="created_at", nullable=false, updatable=false)
    private LocalDateTime createdAt;

    /*
    * 마지막 수정 시각
    * - 제목/내용/공개 범위 변경 시마다 갱신
    * */
    @Column(name="updated_at", nullable=false)
    private LocalDateTime updatedAt;

    //최초 저장 시 자동으로 생성,수정 시각 설정
    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    //업데이트 시 자동으로 수정 시각 갱신
    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /*
    * 필수 값 기반 생성자
    * - visibility가 null이면 기본값 PUBLIC 적용
    * - status는 항상 OPEN으로 시작
    * */
    @Builder
    public QnaQuestion(Long userId, String title, String content, QnaVisibility visibility) {
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.visibility = visibility == null ? QnaVisibility.PUBLIC : visibility;
        this.status = QnaStatus.OPEN;
    }

    /*
    * 질문 수정
    * - 제목, 내용, 공개 범위를 한 번에 갱신
    * - 수정 시 updatedAt은 @PreUpdate에서 자동 처리
    * */
    public void update(String title, String content, QnaVisibility visibility) {
        this.title = title;
        this.content = content;
        this.visibility = visibility;
    }

    /*
    * 답변 완료 상태로 변경
    * - 관리자가 답변을 등록,수정했을 때 호출
    * */
    public void markAnswered() {
        this.status = QnaStatus.ANSWERED;
    }

    /*
    * 다시 답변 대기 상태로 변경
    * - 답변 삭제, 재문의 등의 상황에서 사용 가능
    * */
    public void markOpen() {
        this.status = QnaStatus.OPEN;
    }
}
