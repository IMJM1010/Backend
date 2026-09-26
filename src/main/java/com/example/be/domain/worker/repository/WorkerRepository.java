package com.example.be.domain.worker.repository;

import com.example.be.domain.worker.entity.Worker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
     * Specification 목록 조회({@link #findAll(Specification, Pageable)})에도 반드시 붙인다.
     * OSIV 가 꺼져 있어 Response DTO 변환(컨트롤러)은 트랜잭션 밖에서 일어나므로,
     * 여기서 zone 을 함께 가져오지 않으면 구역이 배정된 작업자가 한 명만 있어도
     * LazyInitializationException 으로 500 이 난다. (batch fetch 는 세션이 열려 있을 때만 동작한다)
     */

    @Override
    @EntityGraph(attributePaths = {"zone", "zone.process"})
    Optional<Worker> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"zone", "zone.process"})
    Page<Worker> findAll(Specification<Worker> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"zone", "zone.process"})
    Page<Worker> findAllByZoneId(Long zoneId, Pageable pageable);

    boolean existsByEmployeeNo(String employeeNo);
}
