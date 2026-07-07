package com.alpaca.security.ratelimit;

import com.alpaca.dto.response.RateLimitResult;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-IP rate limiter using the token bucket algorithm via Bucket4j.
 *
 * <p>A separate {@link Bucket} is maintained for each client IP address. Each bucket is lazily
 * created on first request and starts full. The bucket is refilled at the configured rate (default
 * 500 requests per minute) using a greedy refill strategy.
 *
 * <p><strong>Thread safety:</strong> The underlying {@link ConcurrentHashMap} and Bucket4j's
 * built-in locking guarantee safe concurrent access across multiple requests.
 *
 * <p><strong>Memory management:</strong> Buckets are created on demand and never explicitly
 * evicted. Under steady traffic, the number of buckets stabilizes to the number of active IPs.
 * Burst traffic from distinct IPs will cause temporary memory growth.
 *
 * @see Bucket
 * @see ConsumptionProbe
 * @see RateLimitResult
 */
@Component
public class IPRateLimit {

    @Value("${security.ratelimit.max.rpm:500}")
    private int maxRequests;

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public IPRateLimit(@Value("${security.ratelimit.max.rpm:500}") int maxRequests) {
        this.maxRequests = maxRequests;
    }

    /**
     * Checks whether a request from the given IP is allowed.
     *
     * <p>On first request from an IP, a new full bucket is created. Each request consumes one
     * token. Returns a {@link RateLimitResult} indicating whether the request was allowed, and if
     * denied, how many seconds to wait before the next retry.
     *
     * @param ip the client IP address
     * @return result with consumption status and retry-after seconds when denied
     */
    public RateLimitResult check(String ip) {
        Bucket bucket = buckets.computeIfAbsent(ip, this::createBucket);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            return new RateLimitResult(true, 0);
        }

        return new RateLimitResult(
                false, Duration.ofNanos(probe.getNanosToWaitForRefill()).getSeconds());
    }

    /**
     * Creates a new token bucket with the configured capacity and greedy refill strategy.
     *
     * <p>The bucket starts full ({@code maxRequests} tokens) and refills at a rate of {@code
     * maxRequests} per minute. This allows bursts up to {@code maxRequests} within the window,
     * followed by a steady refill.
     *
     * @param ignoreIp unused — kept for method reference compatibility with {@link
     *     Map#computeIfAbsent}
     * @return a new configured {@link Bucket}
     */
    private Bucket createBucket(String ignoreIp) {
        return Bucket.builder()
                .addLimit(limit -> limit.capacity(maxRequests).refillGreedy(maxRequests, WINDOW))
                .build();
    }
}
