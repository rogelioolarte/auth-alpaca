package com.alpaca.service.impl;

import com.alpaca.service.DataService;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Main Service to initialize actions to use the application.
 *
 * <p>It is implemented from {@link ApplicationRunner} to initialize at the beginning.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
public class InitializerServiceImpl implements ApplicationRunner {

    private final DataService dataService;

    @Override
    @Generated
    public void run(ApplicationArguments args) {
        dataService.initializeData();
    }
}
