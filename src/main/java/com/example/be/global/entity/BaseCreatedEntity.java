package com.example.be.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * created_at 만 가지는 로그성 엔티티의 부모.
 *
 * <p>한 번 기록되면 수정되지 않는 이력 데이터이므로 ERD 에도 updated_at 이 없다.
 *
 * <p>대상: vital_records, env_records, alerts, alert_workers,
 * ai_insights, emergency_actions
 *
 * <p>참고: emergency_actions 는 created_at 대신 executed_at 을 쓰므로 이 클래스를 상속하지 않는다.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseCreatedEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
