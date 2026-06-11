package com.alpaca.security.ratelimit;

import com.alpaca.dto.response.RateLimitResult;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class IPRateLimit {

    @Value("${security.ratelimit.max.rpm:500}")
    private int maxRequests;

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public IPRateLimit(@Value("${security.ratelimit.max.rpm:500}") int maxRequests) {
        this.maxRequests = maxRequests;
    }

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
                .addLimit(limit ->
                        limit.capacity(maxRequests).refillGreedy(maxRequests, WINDOW))
                .build();
    }
}
