package com.example.be.domain.process.entity;

import com.example.be.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공정. 현장을 나누는 가장 큰 단위이며 하위에 구역(zones)을 가진다.
 *
 * <p><b>주의:</b> 클래스명이 {@code java.lang.Process} 와 같다. 다른 패키지에서 쓸 때는
 * 반드시 {@code com.example.be.domain.process.entity.Process} 를 import 할 것.
 * IDE 가 자동으로 {@code java.lang.Process} 를 import 하면 엉뚱한 컴파일 에러가 난다.
 */
@Entity
@Table(name = "processes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Process extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "process_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProcessStatus status;

    @Builder
    private Process(String name, ProcessStatus status) {
        this.name = name;
        this.status = status != null ? status : ProcessStatus.RUNNING;
    }

    /* ---------- 상태 변경 ---------- */

    /** PATCH 이므로 null 인 값은 변경하지 않는다. */
    public void update(String name, ProcessStatus status) {
        if (name != null) {
            this.name = name;
        }
        if (status != null) {
            this.status = status;
        }
    }

    /** 긴급조치(PROCESS_STOP)에서 호출한다. */
    public void stop() {
        this.status = ProcessStatus.STOPPED;
    }

    public void resume() {
        this.status = ProcessStatus.RUNNING;
    }

    public boolean isStopped() {
        return this.status == ProcessStatus.STOPPED;
    }
}
