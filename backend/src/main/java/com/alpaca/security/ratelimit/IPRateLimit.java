package com.alpaca.security.ratelimit;

import com.alpaca.dto.response.RateLimitResult;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IPRateLimit {

    private static final int MAX_REQUESTS = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitResult check(String ip) {
        Bucket bucket = buckets.computeIfAbsent(ip, this::createBucket);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            return new RateLimitResult(true, 0);
        }

        long retryAfterSeconds = Duration.ofNanos(probe.getNanosToWaitForRefill()).getSeconds();

        return new RateLimitResult(false, retryAfterSeconds);
    }

    private Bucket createBucket(String ip) {
        return Bucket.builder()
                .addLimit(limit -> limit.capacity(MAX_REQUESTS).refillGreedy(MAX_REQUESTS, WINDOW))
                .build();
    }
}
