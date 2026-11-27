package io.sourceripple.shared.infrastructure;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class RateLimiter {

    static final int CLEANUP_EVERY = 1_000;

    private final Clock clock;
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AtomicLong calls = new AtomicLong();

    public RateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Records one request for the key and returns how long to wait when the limit is already reached. */
    public Optional<Duration> tryAcquire(String key, int limit, Duration window) {
        Instant now = clock.instant();
        if (calls.incrementAndGet() % CLEANUP_EVERY == 0) {
            dropIdleBuckets(now);
        }

        AtomicReference<Duration> wait = new AtomicReference<>();
        buckets.compute(key, (ignored, existing) -> {
            Bucket bucket = new Bucket(window, existing == null ? new ArrayDeque<>() : existing.hits());
            bucket.prune(now);
            if (bucket.hits().size() >= limit) {
                wait.set(Duration.between(now, bucket.hits().peekFirst().plus(window)));
            } else {
                bucket.hits().addLast(now);
            }
            return bucket;
        });
        return Optional.ofNullable(wait.get());
    }

    int trackedKeys() {
        return buckets.size();
    }

    private void dropIdleBuckets(Instant now) {
        for (String key : buckets.keySet()) {
            buckets.computeIfPresent(key, (ignored, bucket) -> {
                bucket.prune(now);
                return bucket.hits().isEmpty() ? null : bucket;
            });
        }
    }

    private record Bucket(Duration window, Deque<Instant> hits) {

        void prune(Instant now) {
            Instant cutoff = now.minus(window);
            while (!hits.isEmpty() && !hits.peekFirst().isAfter(cutoff)) {
                hits.pollFirst();
            }
        }
    }
}
