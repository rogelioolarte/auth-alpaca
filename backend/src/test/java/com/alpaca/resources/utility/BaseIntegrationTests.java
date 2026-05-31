package com.alpaca.resources.utility;

import jakarta.transaction.Transactional;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@ActiveProfiles("test")
@Import(TestContainersConfiguration.class)
public abstract class BaseIntegrationTests {}
