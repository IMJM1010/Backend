package com.example.be.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 활성화.
 * BaseTimeEntity / BaseCreatedEntity 의 @CreatedDate, @LastModifiedDate 가 동작하려면 필요하다.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
