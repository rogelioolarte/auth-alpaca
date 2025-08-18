package com.alpaca.service.impl;

import com.alpaca.service.DataService;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Spring Boot startup initializer for development environments.
 *
 * <p>This component implements {@link ApplicationRunner}, making it execute startup logic once the
 * Spring application context is fully initialized.
 *
 * <p>Annotated with {@code @Profile("dev")}, this initializer only runs in development mode,
 * executing custom initialization logic via {@link DataService}.
 *
 * @see ApplicationRunner
 * @see DataService
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
public class InitializerServiceImpl implements ApplicationRunner {

    private final DataService dataService;

    /**
     * Called automatically after application startup to initialize application data.
     *
     * @param args the {@link ApplicationArguments} passed to the application
     */
    @Override
    @Generated
    public void run(ApplicationArguments args) {
        dataService.initializeData();
    }
}
