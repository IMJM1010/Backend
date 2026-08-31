package com.example.be.global.config;

import com.example.be.domain.manager.entity.Manager;
import com.example.be.domain.manager.entity.ManagerRole;
import com.example.be.domain.manager.repository.ManagerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 로컬 개발용 초기 계정 생성.
 *
 * <p>관리자 등록 API 는 ADMIN 권한이 필요한데 최초에는 ADMIN 계정 자체가 없어서
 * 아무도 로그인할 수 없다. 그 순환을 끊기 위해 로컬에서만 기본 계정을 하나 만든다.
 *
 * <p><b>local 프로파일에서만 동작한다.</b> 운영 프로파일에서는 빈 자체가 등록되지 않는다.
 */
@Slf4j
@Configuration
@Profile("local")
@RequiredArgsConstructor
public class LocalDataInitializer {

    private static final String DEFAULT_ADMIN_LOGIN_ID = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin1234!";

    private final ManagerRepository managerRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public ApplicationRunner initLocalAdmin() {
        return args -> {
            if (managerRepository.existsByLoginId(DEFAULT_ADMIN_LOGIN_ID)) {
                return;
            }

            managerRepository.save(Manager.builder()
                    .loginId(DEFAULT_ADMIN_LOGIN_ID)
                    .password(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD))
                    .name("로컬 관리자")
                    .role(ManagerRole.ADMIN)
                    .build());

            log.warn("""
                    
                    ====================================================
                     로컬 개발용 ADMIN 계정을 생성했습니다.
                       login_id : {}
                       password : {}
                     운영 환경에서는 절대 이 계정을 사용하지 마세요.
                    ====================================================""",
                    DEFAULT_ADMIN_LOGIN_ID, DEFAULT_ADMIN_PASSWORD);
        };
    }
}
