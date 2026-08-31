package com.example.be.domain.wearabledevice.repository;

import com.example.be.domain.wearabledevice.entity.DeviceType;
import com.example.be.domain.wearabledevice.entity.WearableDevice;
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

    @EntityGraph(attributePaths = "worker")
    List<WearableDevice> findAllByWorkerId(Long workerId);

    boolean existsBySerialNo(String serialNo);

    /** 한 작업자가 같은 종류를 둘 이상 착용하지 못하도록 막는다. */
    boolean existsByWorkerIdAndDeviceType(Long workerId, DeviceType deviceType);

    boolean existsByWorkerIdAndDeviceTypeAndIdNot(Long workerId, DeviceType deviceType, Long id);
}
