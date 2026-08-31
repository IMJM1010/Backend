package com.example.be.domain.worker.entity;

import com.example.be.domain.zone.entity.Zone;
import com.example.be.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 작업자. 관리 대상이며 로그인 주체가 아니다. (로그인은 {@code Manager} 만)
 */
@Entity
@Table(
        name = "workers",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_workers_employee_no", columnNames = "employee_no")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Worker extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "worker_id")
    private Long id;

    /** 현재 위치한 구역. 아직 배정되지 않았거나 구역을 벗어난 상태면 null. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    /**
     * 사번.
     *
     * <p>ERD 에 유니크 제약은 없지만 걸었다. 사번이 중복되면 현장에서 사람을 특정할 수 없고,
     * 근태·생체 기록이 누구 것인지 구분되지 않는다.
     */
    @Column(name = "employee_no", nullable = false, length = 30)
    private String employeeNo;

    @Column(name = "name", nullable = false, length = 30)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "safety_status", nullable = false, length = 20)
    private SafetyStatus safetyStatus;

    /** 재직 여부. 퇴사 처리는 행 삭제가 아니라 이 값을 false 로 바꾼다. */
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Builder
    private Worker(Zone zone, String employeeNo, String name, SafetyStatus safetyStatus) {
        this.zone = zone;
        this.employeeNo = employeeNo;
        this.name = name;
        this.safetyStatus = safetyStatus != null ? safetyStatus : SafetyStatus.NORMAL;
        this.active = true;
    }

    /* ---------- 상태 변경 ---------- */

    /** PATCH 이므로 null 인 값은 변경하지 않는다. */
    public void update(String name) {
        if (name != null) {
            this.name = name;
        }
    }

    /**
     * 구역 이동. null 을 넘기면 구역 미배정 상태가 된다.
     *
     * <p>update 계열과 달리 null 을 "변경 없음"이 아니라 "미배정"으로 해석한다.
     * 전용 엔드포인트라 의도가 분명하기 때문이다.
     */
    public void moveTo(Zone zone) {
        this.zone = zone;
    }

    /** 디바이스·센서 판정 결과를 반영한다. */
    public void changeSafetyStatus(SafetyStatus safetyStatus) {
        this.safetyStatus = safetyStatus;
    }

    /** 퇴사 처리. 근태·생체 기록의 주인으로 남아 있어야 하므로 행을 지우지 않는다. */
    public void resign() {
        this.active = false;
        // 퇴사자가 현장 인원 집계나 구역별 현황판에 남지 않도록 구역에서도 뺀다.
        this.zone = null;
        this.safetyStatus = SafetyStatus.NORMAL;
    }
}
