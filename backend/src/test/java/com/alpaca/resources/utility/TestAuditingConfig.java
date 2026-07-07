package com.alpaca.resources.utility;

import java.util.Optional;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@TestConfiguration
@EnableJpaAuditing(auditorAwareRef = "testAuditorAware")
public class TestAuditingConfig {

    private static final ThreadLocal<String> AUDITOR = ThreadLocal.withInitial(() -> "test-user");

    public static void setAuditor(String auditor) {
        AUDITOR.set(auditor);
    }

    public static void clearAuditor() {
        AUDITOR.remove();
    }

    @Bean
    @Primary
    public AuditorAware<String> testAuditorAware() {
        return () -> Optional.ofNullable(AUDITOR.get());
    }
}
