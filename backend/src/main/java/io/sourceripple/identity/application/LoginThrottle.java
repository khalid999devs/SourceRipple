package io.sourceripple.identity.application;

import io.sourceripple.shared.api.ErrorCode;
import io.sourceripple.shared.api.error.status.RateLimitedException;
import io.sourceripple.shared.infrastructure.RateLimiter;
import io.sourceripple.shared.infrastructure.config.SourceRippleProperties;
import org.springframework.stereotype.Component;

@Component
public class LoginThrottle {

    private final RateLimiter rateLimiter;
    private final SourceRippleProperties.Limit limit;

    public LoginThrottle(RateLimiter rateLimiter, SourceRippleProperties properties) {
        this.rateLimiter = rateLimiter;
        this.limit = properties.rateLimit().login();
    }

    public void check(String clientAddress, String email) {
        acquire("login:ip:" + clientAddress);
        acquire("login:email:" + EmailAddresses.normalize(email));
    }

    private void acquire(String key) {
        rateLimiter.tryAcquire(key, limit.requests(), limit.window()).ifPresent(wait -> {
            throw new RateLimitedException(ErrorCode.RATE_LIMITED, "Too many login attempts. Try again later.", wait);
        });
    }
}
