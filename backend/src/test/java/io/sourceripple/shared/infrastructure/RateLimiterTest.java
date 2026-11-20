package io.sourceripple.shared.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-20T10:00:00Z"));
    private final RateLimiter limiter = new RateLimiter(clock);

    @Test
    void allowsUpToTheLimitThenReportsTheWait() {
        for (int i = 0; i < 3; i++) {
            assertThat(limiter.tryAcquire("key", 3, WINDOW)).isEmpty();
            clock.advance(Duration.ofMinutes(1));
        }

        assertThat(limiter.tryAcquire("key", 3, WINDOW)).contains(Duration.ofMinutes(12));
    }

    @Test
    void freesASlotWhenTheOldestRequestLeavesTheWindow() {
        limiter.tryAcquire("key", 1, WINDOW);
        clock.advance(WINDOW);

        assertThat(limiter.tryAcquire("key", 1, WINDOW)).isEmpty();
    }

    @Test
    void keysAreIndependent() {
        limiter.tryAcquire("a", 1, WINDOW);

        assertThat(limiter.tryAcquire("a", 1, WINDOW)).isPresent();
        assertThat(limiter.tryAcquire("b", 1, WINDOW)).isEmpty();
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
