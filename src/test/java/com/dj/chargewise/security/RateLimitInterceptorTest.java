package com.dj.chargewise.security;

import com.dj.chargewise.config.ChargewiseProperties;
import com.dj.chargewise.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitInterceptorTest {

    private final RateLimitInterceptor interceptor = new RateLimitInterceptor(new ChargewiseProperties(
            null, null, null, null, new ChargewiseProperties.RateLimit(2, Duration.ofMinutes(1)), null));

    @Test
    void allowsRequestsWithinTheLimitAndReportsRemaining() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(requestFrom("agent"), response, new Object())).isTrue();
        assertThat(response.getHeader(RateLimitInterceptor.LIMIT_HEADER)).isEqualTo("2");
        assertThat(response.getHeader(RateLimitInterceptor.REMAINING_HEADER)).isEqualTo("1");
    }

    @Test
    void rejectsRequestsOverTheLimitWithRetryAfter() {
        interceptor.preHandle(requestFrom("agent"), new MockHttpServletResponse(), new Object());
        interceptor.preHandle(requestFrom("agent"), new MockHttpServletResponse(), new Object());

        assertThatThrownBy(() -> interceptor.preHandle(requestFrom("agent"), new MockHttpServletResponse(), new Object()))
                .isInstanceOfSatisfying(RateLimitExceededException.class, ex ->
                        assertThat(ex.getHeaders()).containsKey(HttpHeaders.RETRY_AFTER));
    }

    @Test
    void keepsSeparateLimitsPerUser() {
        interceptor.preHandle(requestFrom("agent"), new MockHttpServletResponse(), new Object());
        interceptor.preHandle(requestFrom("agent"), new MockHttpServletResponse(), new Object());

        assertThat(interceptor.preHandle(requestFrom("other-agent"), new MockHttpServletResponse(), new Object()))
                .isTrue();
    }

    private static MockHttpServletRequest requestFrom(String username) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/disputes/analyse");
        request.setUserPrincipal(() -> username);
        return request;
    }
}
