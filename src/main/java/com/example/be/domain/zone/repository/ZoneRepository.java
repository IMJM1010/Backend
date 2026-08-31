package com.example.be.domain.zone.repository;

import com.example.be.domain.zone.entity.Zone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * 구역에 남아 있는 재직 작업자 수. 구역 삭제 가드에 쓴다.
     *
     * <p>{@code ProcessRepository.countZonesOf} 와 같은 패턴이다. 부모(구역)가 자식(작업자)
     * 서비스를 주입하면 순환 의존이 생기므로, 부모 쪽 리포지토리에서 JPQL 로 해결한다.
     */
    @Query("select count(w) from Worker w where w.zone.id = :zoneId and w.active = true")
    long countActiveWorkersOf(@Param("zoneId") Long zoneId);
}
