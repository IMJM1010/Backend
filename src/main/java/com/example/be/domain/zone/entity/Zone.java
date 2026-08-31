package com.example.be.domain.zone.entity;

import com.example.be.domain.process.entity.Process;
import com.example.be.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.math.BigDecimal;

/**
 * 구역. 공정을 물리적으로 나눈 단위이며 작업자·환경센서·알림·사고가 모두 구역에 매인다.
 *
 * <p>{@code map_x}, {@code map_y} 는 대시보드 현장 지도에 구역을 찍기 위한 좌표다.
 */
@Entity
@Table(
        name = "zones",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_zones_process_zone_code",
                columnNames = {"process_id", "zone_code"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Zone extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "zone_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_id", nullable = false)
    private Process process;

    /**
     * 구역 코드 (A-1, B-3 등).
     *
     * <p>ERD 에 유니크 제약은 없지만 <b>같은 공정 안에서는 중복될 수 없도록</b> 제약을 걸었다.
     * 현장에서 "A-1 구역"이라고 부를 때 어느 구역인지 하나로 특정되어야 하기 때문이다.
     * 다른 공정끼리는 같은 코드를 써도 된다.
     */
    @Column(name = "zone_code", nullable = false, length = 10)
    private String zoneCode;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "map_x", precision = 8, scale = 2)
    private BigDecimal mapX;

    @Column(name = "map_y", precision = 8, scale = 2)
    private BigDecimal mapY;

    @Builder
    private Zone(Process process, String zoneCode, String name, BigDecimal mapX, BigDecimal mapY) {
        this.process = process;
        this.zoneCode = zoneCode;
        this.name = name;
        this.mapX = mapX;
        this.mapY = mapY;
    }

    /* ---------- 상태 변경 ---------- */

    /** PATCH 이므로 null 인 값은 변경하지 않는다. */
    public void update(String zoneCode, String name, BigDecimal mapX, BigDecimal mapY) {
        if (zoneCode != null) {
            this.zoneCode = zoneCode;
        }
        if (name != null) {
            this.name = name;
        }
        if (mapX != null) {
            this.mapX = mapX;
        }
        if (mapY != null) {
            this.mapY = mapY;
        }
    }

    /** 구역을 다른 공정으로 옮긴다. */
    public void moveTo(Process process) {
        this.process = process;
    }
}
