package com.example.be.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * created_at / updated_at 를 모두 가지는 마스터성 엔티티의 부모.
 *
 * <p>대상: managers, processes, zones, workers, attendances, wearable_devices,
 * env_sensors, incidents, dashboard_widgets
 *
 * <p>updated_at 이 없는 로그성 테이블은 {@link BaseCreatedEntity} 를 상속할 것.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
