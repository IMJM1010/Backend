package com.example.be.domain.worker.repository;

import com.example.be.domain.worker.entity.Worker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface WorkerRepository
        extends JpaRepository<Worker, Long>, JpaSpecificationExecutor<Worker> {

    /*
     * 응답에 구역 코드와 공정명을 함께 내려주므로 zone, zone.process 를 함께 조회한다.
     * 없으면 목록 20건 조회에 쿼리가 40번 더 나간다. (N+1)
     *
     * 필터 조합이 많은 목록 조회는 Specification 을 쓰는데, Specification 조회에는
     * @EntityGraph 를 붙이지 않는다. 대신 application.properties 의
     * hibernate.default_batch_fetch_size=100 이 지연 로딩을 IN 절로 묶어주므로
     * 추가 쿼리가 건수만큼이 아니라 연관관계당 1~2회로 끝난다.
     */

    @Override
    @EntityGraph(attributePaths = {"zone", "zone.process"})
    Optional<Worker> findById(Long id);

    @EntityGraph(attributePaths = {"zone", "zone.process"})
    Page<Worker> findAllByZoneId(Long zoneId, Pageable pageable);

    boolean existsByEmployeeNo(String employeeNo);
}
