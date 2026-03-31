package com.example.rungame.qna.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/*
* QnA 답변 엔티티
* - 하나의 질문에 대해 관리자가 남긴 답변 1건을 표현하는 도메인 객체
*
* - 한 질문에는 답변이 최대 1개만 존재(unique 제약으로 보장)
* */
@Entity
@Table(name="qna_answer",
        uniqueConstraints = @UniqueConstraint(name="uk_qna_answer_question", columnNames="question_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QnaAnswer {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
    * 연결된 질문
    * - 반드시 존재해야함 -> optional = false
    * - 한 질문당 하나의 답변만 허용
    * - 지연 로딩으로 필요할 때만 질문 엔티티를 조회
    * */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="question_id", nullable=false)
    private QnaQuestion question;

    //답변 작성한 관리자 아이디
    @Column(name="admin_id", nullable=false)
    private Long adminId;

    /*
    * 답변 내용
    * - @Lob 으로 길이 제한 없이 저장 가능
    * */
    @Lob
    @Column(nullable=false)
    private String content;

    /*
    * 답변 생성 시각
    * - 최초 저장 시점에만 설정
    * */
    @Column(name="created_at", nullable=false, updatable=false)
    private LocalDateTime createdAt;

    /*
    * 마지막 수정 시각
    * - 내용 변경 시마다 갱신
    * */
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

    /*
    * 필수 값을 받는 생성자
    * - @Builder로 가독성 있게 생성하도록 설계
    * */
    @Builder
    public QnaAnswer(QnaQuestion question, Long adminId, String content) {
        this.question = question;
        this.adminId = adminId;
        this.content = content;
    }

    /*
    * 답변 내용 수정 메서드
    * - 비즈니스 로직에서 내용만 바꾸고 싶을 때 사용
    * - updatedAt은 @PreUpdate에 의해 자동 갱신
    * */
    public void updateContent(String content) {
        this.content = content;
    }
}
