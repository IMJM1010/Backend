package com.example.be.domain.process.repository;

import com.example.be.domain.process.entity.Process;
import com.example.be.domain.process.entity.ProcessStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessRepository extends JpaRepository<Process, Long> {

    Page<Process> findAllByStatus(ProcessStatus status, Pageable pageable);

    /**
     * 하위 구역 존재 여부.
     *
     * <p>{@code Process} 에 {@code @OneToMany zones} 를 두면 이 확인은 쉬워지지만
     * 양방향 연관관계가 생기고 삭제 확인만을 위해 컬렉션 전체를 로딩하게 된다.
     * 카운트 쿼리 하나로 끝내기 위해 JPQL 로 Zone 을 참조한다.
     */
    @Query("select count(z) from Zone z where z.process.id = :processId")
    long countZonesOf(@Param("processId") Long processId);
}
