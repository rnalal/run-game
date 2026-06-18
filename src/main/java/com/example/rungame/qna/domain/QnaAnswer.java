package com.example.rungame.qna.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

//QnA 답변 엔티티
@Entity
@Table(name="qna_answer",
        uniqueConstraints = @UniqueConstraint(name="uk_qna_answer_question", columnNames="question_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QnaAnswer {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //연결된 질문
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="question_id", nullable=false)
    private QnaQuestion question;

    //답변 작성한 관리자 아이디
    @Column(name="admin_id", nullable=false)
    private Long adminId;

    //답변 내용
    @Lob
    @Column(nullable=false)
    private String content;

    //답변 생성 시각
    @Column(name="created_at", nullable=false, updatable=false)
    private LocalDateTime createdAt;

    //마지막 수정 시각
    @Column(name="updated_at", nullable=false)
    private LocalDateTime updatedAt;

    //최초 저장 시 자동으로 생성,수정 시각 셋업
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

    //필수 값을 받는 생성자
    @Builder
    public QnaAnswer(QnaQuestion question, Long adminId, String content) {
        this.question = question;
        this.adminId = adminId;
        this.content = content;
    }

    //답변 내용 수정 메서드
    public void updateContent(String content) {
        this.content = content;
    }
}
