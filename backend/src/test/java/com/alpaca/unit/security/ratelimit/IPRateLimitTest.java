package com.alpaca.unit.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.alpaca.dto.response.RateLimitResult;
import com.alpaca.security.ratelimit.IPRateLimit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IPRateLimitTest {

    private IPRateLimit ipRateLimit;
    private final String testIp = "192.168.1.1";

    @BeforeEach
    void setUp() {
        // A fresh instance for each test to clean the internal ConcurrentHashMap
        ipRateLimit = new IPRateLimit();
    }

    @Test
    @DisplayName("It must allow requests below the limit")
    void shouldAllowRequestWithinLimit() {
        RateLimitResult result = ipRateLimit.check(testIp);

        assertThat(result.allowed()).isTrue();
        assertThat(result.retryAfterSeconds()).isZero();
    }

    @Test
    @DisplayName("You must block requests when the limit of 10 is exceeded.")
    void shouldBlockRequestWhenLimitExceeded() {
        for (int i = 0; i < 10; i++) {
            ipRateLimit.check(testIp);
        }

        RateLimitResult result = ipRateLimit.check(testIp);

        assertThat(result.allowed()).isFalse();
        assertThat(result.retryAfterSeconds()).isGreaterThan(0);
    }

    @Test
    @DisplayName("You must manage separate buckets for different IPs")
    void shouldMaintainSeparateBucketsForDifferentIps() {
        String otherIp = "10.0.0.1";

        for (int i = 0; i < 10; i++) {
            ipRateLimit.check(testIp);
        }

        assertThat(ipRateLimit.check(testIp).allowed()).isFalse();
        assertThat(ipRateLimit.check(otherIp).allowed()).isTrue();
    }
}
