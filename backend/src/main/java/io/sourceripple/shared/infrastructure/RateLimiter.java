package io.sourceripple.shared.infrastructure;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class RateLimiter {

    private static final int CLEANUP_EVERY = 1_000;

    private final Clock clock;
    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();
    private final AtomicLong calls = new AtomicLong();

    public RateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Records one request for the key and returns how long to wait when the limit is already reached. */
    public Optional<Duration> tryAcquire(String key, int limit, Duration window) {
        Instant now = clock.instant();
        if (calls.incrementAndGet() % CLEANUP_EVERY == 0) {
            hits.values().removeIf(log -> isEmptyAfterPruning(log, now.minus(window)));
        }
        Deque<Instant> log = hits.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (log) {
            prune(log, now.minus(window));
            if (log.size() >= limit) {
                return Optional.of(Duration.between(now, log.peekFirst().plus(window)));
            }
            log.addLast(now);
            return Optional.empty();
        }
    }

    private static boolean isEmptyAfterPruning(Deque<Instant> log, Instant cutoff) {
        synchronized (log) {
            prune(log, cutoff);
            return log.isEmpty();
        }
    }

    private static void prune(Deque<Instant> log, Instant cutoff) {
        while (!log.isEmpty() && !log.peekFirst().isAfter(cutoff)) {
            log.pollFirst();
        }
    }
}
