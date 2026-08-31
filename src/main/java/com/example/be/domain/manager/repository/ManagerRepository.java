package com.example.be.domain.manager.repository;

import com.example.be.domain.manager.entity.Manager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ManagerRepository
        extends JpaRepository<Manager, Long>, JpaSpecificationExecutor<Manager> {

    Optional<Manager> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);
}
