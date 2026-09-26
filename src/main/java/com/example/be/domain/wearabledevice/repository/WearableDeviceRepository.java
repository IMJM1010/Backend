package com.example.be.domain.wearabledevice.repository;

import com.example.be.domain.wearabledevice.entity.DeviceType;
import com.example.be.domain.wearabledevice.entity.WearableDevice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface WearableDeviceRepository
        extends JpaRepository<WearableDevice, Long>, JpaSpecificationExecutor<WearableDevice> {

    @Override
    @EntityGraph(attributePaths = "worker")
    Optional<WearableDevice> findById(Long id);

    /**
     * 필터 목록 조회. 응답에 작업자 이름·사번을 담으므로 worker 를 함께 가져온다.
     *
     * <p>OSIV 가 꺼져 있어 DTO 변환은 트랜잭션 밖에서 일어난다. 이게 없으면 작업자가 배정된
     * 디바이스가 하나만 있어도 LazyInitializationException 으로 500 이 난다.
     */
    @Override
    @EntityGraph(attributePaths = "worker")
    Page<WearableDevice> findAll(Specification<WearableDevice> spec, Pageable pageable);

    @EntityGraph(attributePaths = "worker")
    List<WearableDevice> findAllByWorkerId(Long workerId);

    boolean existsBySerialNo(String serialNo);

    /** 한 작업자가 같은 종류를 둘 이상 착용하지 못하도록 막는다. */
    boolean existsByWorkerIdAndDeviceType(Long workerId, DeviceType deviceType);

    boolean existsByWorkerIdAndDeviceTypeAndIdNot(Long workerId, DeviceType deviceType, Long id);
}
