package com.example.be.domain.zone.repository;

import com.example.be.domain.zone.entity.Zone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ZoneRepository extends JpaRepository<Zone, Long> {

    /*
     * 응답에 공정명을 함께 내려주므로 process 를 매번 함께 조회한다.
     * @EntityGraph 가 없으면 목록 20건 조회 시 공정 조회 쿼리가 20번 더 나간다. (N+1)
     */

    @Override
    @EntityGraph(attributePaths = "process")
    Page<Zone> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "process")
    Optional<Zone> findById(Long id);

    @EntityGraph(attributePaths = "process")
    Page<Zone> findAllByProcessId(Long processId, Pageable pageable);

    boolean existsByProcessIdAndZoneCode(Long processId, String zoneCode);

    /** 수정 시 자기 자신은 중복 검사에서 제외한다. */
    boolean existsByProcessIdAndZoneCodeAndIdNot(Long processId, String zoneCode, Long id);
}
