package com.alpaca.resources.utility;

import com.alpaca.config.JpaRepositoryConfig;
import jakarta.transaction.Transactional;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, JpaRepositoryConfig.class})
public abstract class BaseIntegrationTests {}
