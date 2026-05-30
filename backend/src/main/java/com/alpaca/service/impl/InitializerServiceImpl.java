package com.alpaca.service.impl;

import com.alpaca.service.DataService;
import java.util.Arrays;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Spring Boot startup initializer for development environments.
 *
 * <p>This component implements {@link ApplicationRunner}, making it execute startup logic once the
 * Spring application context is fully initialized.
 *
 * @see ApplicationRunner
 * @see DataService
 */
@Component
@RequiredArgsConstructor
public class InitializerServiceImpl implements ApplicationRunner {

    private final DataService dataService;
    private final Environment environment;

    /**
     * Called automatically after application startup to initialize application data.
     *
     * @param args the {@link ApplicationArguments} passed to the application
     */
    @Override
    @Generated
    public void run(@NonNull ApplicationArguments args) {
        dataService.initializeAdminUser();
        if (isDevProfileActive()) {
            dataService.initializeData();
        }
    }

    /** Helper method to check if the "dev" profile is currently active. */
    private boolean isDevProfileActive() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }
}
