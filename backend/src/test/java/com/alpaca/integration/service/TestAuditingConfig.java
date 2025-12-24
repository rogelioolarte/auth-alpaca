package com.alpaca.integration.service;


import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@TestConfiguration
@EnableJpaAuditing(auditorAwareRef = "testAuditor")
public class TestAuditingConfig {

	@Bean
	public AuditorAware<String> testAuditor() {
		return () -> Optional.of("test-user");
	}
}
