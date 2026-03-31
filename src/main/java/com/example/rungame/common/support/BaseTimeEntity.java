package com.example.rungame.common.support;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/*
* 공통 시간 관리 베이스 엔티티
*
* - 모든 JPA 엔티티에서 생성/수정 시간을 자동으로 기록하기 위한 추상 클래스
* - 중복 필드 제거 및 일관성 감사 처리 목적
*
* 사용 방식:
*   public class User extends BaseTimeEntity {...}
* */
@Getter
@MappedSuperclass
// -> 이 클래스 자체는 테이블이 되지 않고 상속한 엔티티의 컬럼으로 포함됨
@EntityListeners(AuditingEntityListener.class)
// -> Spring Data JPA Auditing 기능 활성화
public abstract class BaseTimeEntity {

    /*
    * 엔티티 생성 시각
    * - 최초 저장 시 자동 설정
    * - 이후 수정 불가
    * */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /*
    * 엔티티 마지막 수정 시각
    * - update 발생 시마다 자동 갱신
    * */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}
