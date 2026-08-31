package com.example.be.domain.wearabledevice.entity;

import com.example.be.domain.worker.entity.Worker;
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

import java.time.LocalDateTime;

/**
 * 작업자가 착용하는 웨어러블 디바이스. 생체 기록의 발생원이다.
 */
@Entity
@Table(
        name = "wearable_devices",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_wearable_devices_serial_no", columnNames = "serial_no")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WearableDevice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_id")
    private Long id;

    /** 착용 작업자. 미배정 상태면 null. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id")
    private Worker worker;

    /**
     * 시리얼 번호.
     *
     * <p>ERD 에 유니크 제약은 없지만 걸었다. 같은 시리얼이 둘 있으면 게이트웨이가 보낸
     * 생체 데이터를 어느 디바이스 것으로 기록할지 결정할 수 없다.
     */
    @Column(name = "serial_no", nullable = false, length = 50)
    private String serialNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 30)
    private DeviceType deviceType;

    /** 배터리 잔량 0~100(%). 아직 동기화되지 않았으면 null. */
    @Column(name = "battery_level")
    private Integer batteryLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_status", nullable = false, length = 20)
    private ConnectionStatus connectionStatus;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Builder
    private WearableDevice(Worker worker, String serialNo, DeviceType deviceType) {
        this.worker = worker;
        this.serialNo = serialNo;
        this.deviceType = deviceType;
        // 등록 직후에는 아직 게이트웨이와 연결되기 전이다.
        this.connectionStatus = ConnectionStatus.DISCONNECTED;
    }

    /* ---------- 상태 변경 ---------- */

    /** 착용 작업자 변경. null 을 넘기면 미배정 처리. */
    public void assignTo(Worker worker) {
        this.worker = worker;
    }

    public void changeType(DeviceType deviceType) {
        if (deviceType != null) {
            this.deviceType = deviceType;
        }
    }

    /**
     * 게이트웨이가 주기적으로 보내는 상태 갱신.
     * null 인 값은 이번 보고에 포함되지 않은 것으로 보고 기존 값을 유지한다.
     */
    public void updateStatus(Integer batteryLevel, ConnectionStatus connectionStatus,
                             LocalDateTime lastSyncedAt) {
        if (batteryLevel != null) {
            this.batteryLevel = batteryLevel;
        }
        if (connectionStatus != null) {
            this.connectionStatus = connectionStatus;
        }
        this.lastSyncedAt = lastSyncedAt != null ? lastSyncedAt : LocalDateTime.now();
    }

    /**
     * 폐기·분실 처리. 행을 지우지 않는다.
     *
     * <p>이 디바이스가 남긴 생체 기록이 device_id 로 이 행을 참조하고 있어
     * 실제로 지우면 과거 측정 이력을 전부 잃는다.
     */
    public void retire() {
        this.connectionStatus = ConnectionStatus.RETIRED;
        this.worker = null;
        this.batteryLevel = null;
    }

    public boolean isRetired() {
        return this.connectionStatus.isRetired();
    }
}
