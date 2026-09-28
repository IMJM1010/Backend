package com.example.be.domain.envsensor.entity;

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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 구역에 설치된 환경 센서. 환경 기록의 발생원이다.
 *
 * <p>{@code threshold_min} ~ {@code threshold_max} 가 정상 범위이며, 측정값이 이 범위를 벗어나면
 * 알림 판정 대상이 된다. 둘 중 하나만 있어도 된다. (산소는 하한만, 소음은 상한만 의미가 있다)
 */
@Entity
@Table(
        name = "env_sensors",
        indexes = @Index(name = "idx_env_sensors_zone", columnList = "zone_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EnvSensor extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sensor_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensor_type", nullable = false, length = 20)
    private SensorType sensorType;

    @Column(name = "unit", nullable = false, length = 10)
    private String unit;

    @Column(name = "threshold_min", precision = 10, scale = 2)
    private BigDecimal thresholdMin;

    @Column(name = "threshold_max", precision = 10, scale = 2)
    private BigDecimal thresholdMax;

    @Builder
    private EnvSensor(Zone zone, SensorType sensorType, String unit,
                      BigDecimal thresholdMin, BigDecimal thresholdMax) {
        this.zone = zone;
        this.sensorType = sensorType;
        this.unit = unit != null ? unit : sensorType.getDefaultUnit();
        this.thresholdMin = thresholdMin;
        this.thresholdMax = thresholdMax;
    }

    /* ---------- 상태 변경 ---------- */

    /**
     * 종류·단위 변경. PATCH 이므로 null 은 변경하지 않는다.
     *
     * <p>종류만 바뀌고 단위가 오지 않으면 새 종류의 기본 단위로 맞춘다.
     * 온도 센서를 소음 센서로 바꿨는데 단위가 ℃ 로 남는 일을 막기 위해서다.
     */
    public void changeType(SensorType sensorType, String unit) {
        if (sensorType != null && sensorType != this.sensorType) {
            this.sensorType = sensorType;
            this.unit = unit != null ? unit : sensorType.getDefaultUnit();
            return;
        }
        if (unit != null) {
            this.unit = unit;
        }
    }

    /** 임계값 변경. null 은 변경하지 않는다. 범위 검증은 서비스에서 병합 결과로 한다. */
    public void changeThreshold(BigDecimal thresholdMin, BigDecimal thresholdMax) {
        if (thresholdMin != null) {
            this.thresholdMin = thresholdMin;
        }
        if (thresholdMax != null) {
            this.thresholdMax = thresholdMax;
        }
    }

    /** 다른 구역으로 옮겨 설치한다. */
    public void moveTo(Zone zone) {
        this.zone = zone;
    }
}
