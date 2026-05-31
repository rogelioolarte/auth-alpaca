package com.alpaca.security.ratelimit;

import com.alpaca.dto.response.RateLimitResult;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class IPRateLimit {

    private static final int MAX_REQUESTS = 50000;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitResult check(String ip) {
        Bucket bucket = buckets.computeIfAbsent(ip, this::createBucket);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            return new RateLimitResult(true, 0);
        }

        return new RateLimitResult(
                false, Duration.ofNanos(probe.getNanosToWaitForRefill()).getSeconds());
    }

    private Bucket createBucket(String ignoreIp) {
        return Bucket.builder()
                .addLimit(limit -> limit.capacity(MAX_REQUESTS).refillGreedy(MAX_REQUESTS, WINDOW))
                .build();
    }
}
