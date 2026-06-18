package com.example.rungame.common.support;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

//공통 시간 관리 베이스 엔티티
@Getter
@MappedSuperclass
// -> 이 클래스 자체는 테이블이 되지 않고 상속한 엔티티의 컬럼으로 포함됨
@EntityListeners(AuditingEntityListener.class)
// -> Spring Data JPA Auditing 기능 활성화
public abstract class BaseTimeEntity {

    //엔티티 생성 시각
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    //엔티티 마지막 수정 시각
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}
