package com.example.service.impl;

import com.example.service.DataService;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InitializerServiceImpl implements ApplicationRunner {

    private final DataService dataService;

    @Override
    @Generated
    public void run(ApplicationArguments args) {
        dataService.initializeData();
    }

}
