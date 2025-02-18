package com.example.service.impl;

import com.example.service.DataService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InitializerServiceImpl implements ApplicationRunner {

    private final DataService dataService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        dataService.initializeData();
    }

}
