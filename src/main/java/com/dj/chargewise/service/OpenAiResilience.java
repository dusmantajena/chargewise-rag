package com.dj.chargewise.service;

import com.dj.chargewise.config.ChargewiseProperties;
import com.dj.chargewise.exception.ChargewiseException;
import com.dj.chargewise.exception.LlmUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Component
public class OpenAiResilience {

    private static final Duration RETRY_AFTER_FAILURE = Duration.ofSeconds(10);

    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final TimeLimiter timeLimiter;
    private final TaskExecutor taskExecutor;
    private final Duration openStateDuration;

    public OpenAiResilience(Retry retry, CircuitBreaker circuitBreaker, TimeLimiter timeLimiter,
                            @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor,
                            ChargewiseProperties properties) {
        this.retry = retry;
        this.circuitBreaker = circuitBreaker;
        this.timeLimiter = timeLimiter;
        this.taskExecutor = taskExecutor;
        this.openStateDuration = properties.resilience().openStateDuration();
    }

    public <T> T call(Supplier<T> openAiCall) {
        Callable<T> timeLimited = TimeLimiter.decorateFutureSupplier(timeLimiter,
                () -> CompletableFuture.supplyAsync(openAiCall, taskExecutor));
        Callable<T> guarded = Retry.decorateCallable(retry,
                CircuitBreaker.decorateCallable(circuitBreaker, timeLimited));

        try {
            return guarded.call();
        } catch (ChargewiseException ex) {
            throw ex;
        } catch (CallNotPermittedException ex) {
            throw new LlmUnavailableException(
                    "The AI service is temporarily unavailable after repeated failures. Try again shortly.",
                    openStateDuration, ex);
        } catch (TimeoutException ex) {
            throw new LlmUnavailableException("The AI service did not respond in time.", RETRY_AFTER_FAILURE, ex);
        } catch (Exception ex) {
            throw new LlmUnavailableException("The AI service could not process the request.", RETRY_AFTER_FAILURE, ex);
        }
    }

    public void run(Runnable openAiCall) {
        call(() -> {
            openAiCall.run();
            return null;
        });
    }
}
