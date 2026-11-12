package com.dj.chargewise.config;

import com.dj.chargewise.exception.ChargewiseException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResilienceConfig {

    public static final String OPENAI = "openai";

    private static final Logger log = LoggerFactory.getLogger(ResilienceConfig.class);
    private static final double BACKOFF_MULTIPLIER = 2.0;

    @Bean
    Retry openAiRetry(ChargewiseProperties properties) {
        ChargewiseProperties.Resilience resilience = properties.resilience();
        Retry retry = Retry.of(OPENAI, RetryConfig.custom()
                .maxAttempts(resilience.maxAttempts())
                .intervalFunction(IntervalFunction.ofExponentialBackoff(
                        resilience.initialBackoff(), BACKOFF_MULTIPLIER))
                .retryOnException(ResilienceConfig::isRetryable)
                .build());

        retry.getEventPublisher().onRetry(event -> log.warn("Retrying OpenAI call, attempt {} after: {}",
                event.getNumberOfRetryAttempts(), event.getLastThrowable().toString()));
        return retry;
    }

    @Bean
    CircuitBreaker openAiCircuitBreaker(ChargewiseProperties properties) {
        ChargewiseProperties.Resilience resilience = properties.resilience();
        CircuitBreaker circuitBreaker = CircuitBreaker.of(OPENAI, CircuitBreakerConfig.custom()
                .failureRateThreshold(resilience.failureRateThreshold())
                .slidingWindowSize(resilience.slidingWindowSize())
                .minimumNumberOfCalls(resilience.minimumNumberOfCalls())
                .waitDurationInOpenState(resilience.openStateDuration())
                .permittedNumberOfCallsInHalfOpenState(2)
                .ignoreExceptions(ChargewiseException.class)
                .build());

        circuitBreaker.getEventPublisher().onStateTransition(event -> log.warn("OpenAI circuit breaker {}",
                event.getStateTransition()));
        return circuitBreaker;
    }

    @Bean
    TimeLimiter openAiTimeLimiter(ChargewiseProperties properties) {
        return TimeLimiter.of(OPENAI, TimeLimiterConfig.custom()
                .timeoutDuration(properties.resilience().timeout())
                .cancelRunningFuture(true)
                .build());
    }

    private static boolean isRetryable(Throwable throwable) {
        return !(throwable instanceof ChargewiseException) && !(throwable instanceof CallNotPermittedException);
    }
}
