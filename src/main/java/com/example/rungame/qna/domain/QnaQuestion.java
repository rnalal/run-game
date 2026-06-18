package com.example.rungame.qna.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

//QnA 질문 엔티티
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

    //질문 본문 내용
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

    //질문 생성 시각
    @Column(name="created_at", nullable=false, updatable=false)
    private LocalDateTime createdAt;

    //마지막 수정 시각
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

    //필수 값 기반 생성자
    @Builder
    public QnaQuestion(Long userId, String title, String content, QnaVisibility visibility) {
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.visibility = visibility == null ? QnaVisibility.PUBLIC : visibility;
        this.status = QnaStatus.OPEN;
    }

    //질문 수정
    public void update(String title, String content, QnaVisibility visibility) {
        this.title = title;
        this.content = content;
        this.visibility = visibility;
    }

    //답변 완료 상태로 변경
    public void markAnswered() {
        this.status = QnaStatus.ANSWERED;
    }

    //다시 답변 대기 상태로 변경
    public void markOpen() {
        this.status = QnaStatus.OPEN;
    }
}
