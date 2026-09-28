package com.example.be.domain.envsensor.repository;

import com.example.be.domain.envsensor.entity.EnvSensor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface EnvSensorRepository
        extends JpaRepository<EnvSensor, Long>, JpaSpecificationExecutor<EnvSensor> {

    /*
     * 응답에 구역 코드와 구역명을 담으므로 zone 을 함께 조회한다.
     * OSIV 가 꺼져 있어 DTO 변환은 트랜잭션 밖에서 일어난다. 여기서 zone 을 가져오지 않으면
     * LazyInitializationException 으로 500 이 난다. (Specification 목록 조회 포함)
     */

    @Override
    @EntityGraph(attributePaths = "zone")
    Optional<EnvSensor> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "zone")
    Page<EnvSensor> findAll(Specification<EnvSensor> spec, Pageable pageable);
}
