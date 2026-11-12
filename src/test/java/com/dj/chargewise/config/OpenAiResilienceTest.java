package com.dj.chargewise.config;

import com.dj.chargewise.exception.InsufficientContextException;
import com.dj.chargewise.exception.LlmUnavailableException;
import com.dj.chargewise.service.OpenAiResilience;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.http.HttpHeaders;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiResilienceTest {

    private final ResilienceConfig config = new ResilienceConfig();
    private final AtomicInteger calls = new AtomicInteger();

    private CircuitBreaker circuitBreaker;
    private OpenAiResilience resilience;

    @BeforeEach
    void setUp() {
        ChargewiseProperties properties = new ChargewiseProperties(null, null, null,
                new ChargewiseProperties.Resilience(Duration.ofMillis(300), 3, Duration.ofMillis(10),
                        50f, 4, 4, Duration.ofSeconds(30)),
                null, null);
        circuitBreaker = config.openAiCircuitBreaker(properties);
        resilience = new OpenAiResilience(config.openAiRetry(properties), circuitBreaker,
                config.openAiTimeLimiter(properties), new SimpleAsyncTaskExecutor(), properties);
    }

    @Test
    void succeedsAfterTransientFailures() {
        String result = resilience.call(() -> {
            if (calls.incrementAndGet() < 3) {
                throw new IllegalStateException("Temporary OpenAI error");
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(calls).hasValue(3);
    }

    @Test
    void returnsServiceUnavailableAfterMaxAttempts() {
        assertThatThrownBy(() -> resilience.call(this::alwaysFail))
                .isInstanceOf(LlmUnavailableException.class)
                .hasMessageContaining("could not process");
        assertThat(calls).hasValue(3);
    }

    @Test
    void doesNotRetryBusinessErrors() {
        assertThatThrownBy(() -> resilience.call(() -> {
            calls.incrementAndGet();
            throw new InsufficientContextException("No rules match.");
        })).isInstanceOf(InsufficientContextException.class);

        assertThat(calls).hasValue(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isZero();
    }

    @Test
    void cutsOffSlowCalls() {
        assertThatThrownBy(() -> resilience.call(() -> {
            calls.incrementAndGet();
            sleep(Duration.ofSeconds(2));
            return "too late";
        })).isInstanceOf(LlmUnavailableException.class)
                .hasMessageContaining("did not respond in time");
    }

    @Test
    void opensCircuitAndFailsFastAfterRepeatedFailures() {
        assertThatThrownBy(() -> resilience.call(this::alwaysFail)).isInstanceOf(LlmUnavailableException.class);
        assertThatThrownBy(() -> resilience.call(this::alwaysFail)).isInstanceOf(LlmUnavailableException.class);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        int callsBefore = calls.get();
        assertThatThrownBy(() -> resilience.call(this::alwaysFail))
                .isInstanceOfSatisfying(LlmUnavailableException.class, ex -> assertThat(ex.getHeaders())
                        .containsEntry(HttpHeaders.RETRY_AFTER, "30"))
                .hasMessageContaining("temporarily unavailable");
        assertThat(calls).hasValue(callsBefore);
    }

    private String alwaysFail() {
        calls.incrementAndGet();
        throw new IllegalStateException("OpenAI is down");
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
