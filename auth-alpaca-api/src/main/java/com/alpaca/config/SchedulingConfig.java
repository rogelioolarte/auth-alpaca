package com.alpaca.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's {@link org.springframework.scheduling.annotation.Scheduled @Scheduled} support.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {}
