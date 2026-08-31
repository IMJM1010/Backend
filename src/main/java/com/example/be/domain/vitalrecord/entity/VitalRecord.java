package com.example.be.domain.vitalrecord.entity;

import com.example.be.domain.wearabledevice.entity.WearableDevice;
import com.example.be.global.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 생체 기록. 디바이스가 주기적으로 보내는 시계열 데이터다.
 *
 * <p><b>이 테이블은 다른 테이블과 규모가 다르다.</b> 작업자 100명이 10초마다 보내면
 * 하루 86만 건, 한 달이면 2600만 건이다. 그래서 아래 두 가지를 지켜야 한다.
 * <ul>
 *   <li>조회는 반드시 {@code device_id} + 기간으로 좁힌다. 전체 스캔은 서버를 멈춘다.</li>
 *   <li>수정되지 않는 이력이므로 {@code updated_at} 이 없다. ({@link BaseCreatedEntity})</li>
 * </ul>
 */
@Entity
@Table(
        name = "vital_records",
        indexes = @Index(
                name = "idx_vital_records_device_measured",
                columnList = "device_id, measured_at")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VitalRecord extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vital_record_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private WearableDevice device;

    /** 심박수 (bpm). 센서가 값을 못 읽으면 null. */
    @Column(name = "heart_rate")
    private Integer heartRate;

    /** 체온 (℃). */
    @Column(name = "body_temp", precision = 4, scale = 1)
    private BigDecimal bodyTemp;

    /** 걸음 수. 근무 체력 판단에 쓴다. */
    @Column(name = "step_count")
    private Integer stepCount;

    /**
     * 측정 일시.
     *
     * <p>{@code created_at}(서버 수신 시각)과 다르다. 게이트웨이가 네트워크 장애로 뒤늦게
     * 밀어 넣는 경우가 있어 두 값이 벌어질 수 있고, 조회 기준은 항상 이 값이다.
     */
    @Column(name = "measured_at", nullable = false)
    private LocalDateTime measuredAt;

    @Builder
    private VitalRecord(WearableDevice device, Integer heartRate, BigDecimal bodyTemp,
                        Integer stepCount, LocalDateTime measuredAt) {
        this.device = device;
        this.heartRate = heartRate;
        this.bodyTemp = bodyTemp;
        this.stepCount = stepCount;
        this.measuredAt = measuredAt;
    }
}
