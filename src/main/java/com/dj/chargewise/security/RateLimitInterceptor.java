package com.dj.chargewise.security;

import com.dj.chargewise.config.ChargewiseProperties;
import com.dj.chargewise.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.security.Principal;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    public static final String LIMIT_HEADER = "X-RateLimit-Limit";
    public static final String REMAINING_HEADER = "X-RateLimit-Remaining";

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int capacity;
    private final Duration period;

    public RateLimitInterceptor(ChargewiseProperties properties) {
        this.capacity = properties.rateLimit().capacity();
        this.period = properties.rateLimit().period();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String client = Optional.ofNullable(request.getUserPrincipal())
                .map(Principal::getName)
                .orElse(request.getRemoteAddr());

        ConsumptionProbe probe = buckets.computeIfAbsent(client, key -> newBucket())
                .tryConsumeAndReturnRemaining(1);

        response.setHeader(LIMIT_HEADER, String.valueOf(capacity));
        response.setHeader(REMAINING_HEADER, String.valueOf(probe.getRemainingTokens()));
        if (probe.isConsumed()) {
            return true;
        }

        long retryAfterSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1);
        throw new RateLimitExceededException(capacity, retryAfterSeconds);
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(capacity).refillGreedy(capacity, period).build())
                .build();
    }
}
