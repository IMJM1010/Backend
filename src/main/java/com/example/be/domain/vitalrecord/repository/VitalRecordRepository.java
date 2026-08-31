package com.example.be.domain.vitalrecord.repository;

import com.example.be.domain.vitalrecord.entity.VitalRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VitalRecordRepository extends JpaRepository<VitalRecord, Long> {

    @Override
    @EntityGraph(attributePaths = {"device", "device.worker"})
    Optional<VitalRecord> findById(Long id);

    /**
     * 디바이스 + 기간 조회. {@code idx_vital_records_device_measured} 인덱스를 그대로 탄다.
     */
    @EntityGraph(attributePaths = {"device", "device.worker"})
    Page<VitalRecord> findAllByDeviceIdAndMeasuredAtBetween(
            Long deviceId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    /** 디바이스 지정 없이 기간만으로 조회. 대시보드 전체 집계용이라 기간을 반드시 좁혀서 쓴다. */
    @EntityGraph(attributePaths = {"device", "device.worker"})
    Page<VitalRecord> findAllByMeasuredAtBetween(
            LocalDateTime from, LocalDateTime to, Pageable pageable);

    /**
     * 특정 작업자의 최신 생체 기록.
     *
     * <p>{@code Pageable} 에 {@code PageRequest.of(0, 1)} 을 넘겨 1건만 가져온다.
     * 전체를 정렬해서 받아온 뒤 자바에서 첫 건을 고르면 데이터가 쌓일수록 감당할 수 없다.
     */
    @Query("""
            select vr from VitalRecord vr
            join fetch vr.device d
            join fetch d.worker w
            where w.id = :workerId
            order by vr.measuredAt desc
            """)
    List<VitalRecord> findLatestByWorkerId(@Param("workerId") Long workerId, Pageable pageable);

    long countByDeviceId(Long deviceId);
}
