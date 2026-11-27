package io.sourceripple.source.infrastructure.fetch;

import io.sourceripple.shared.domain.GlobalFetchLimits;
import io.sourceripple.source.application.RobotsPolicyService;
import io.sourceripple.source.application.fetch.FetchPurpose;
import io.sourceripple.source.application.fetch.FetchRequest;
import io.sourceripple.source.application.fetch.FetchResult;
import io.sourceripple.source.application.fetch.WebFetcher;
import io.sourceripple.source.application.fetch.failure.SourceFetchFailedException;
import io.sourceripple.source.domain.CanonicalUrl;
import io.sourceripple.source.domain.UrlCanonicalizer;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class SafeFetchProxy implements WebFetcher {

    private final WebFetcher delegate;
    private final UrlCanonicalizer canonicalizer;
    private final RobotsPolicyService robots;
    private final GlobalFetchLimits limits;
    private final Semaphore permits;

    public SafeFetchProxy(
            WebFetcher delegate,
            UrlCanonicalizer canonicalizer,
            RobotsPolicyService robots,
            GlobalFetchLimits limits) {
        this.delegate = delegate;
        this.canonicalizer = canonicalizer;
        this.robots = robots;
        this.limits = limits;
        this.permits = new Semaphore(limits.concurrency(), true);
    }

    @Override
    public FetchResult fetch(FetchRequest request) {
        long started = System.nanoTime();
        long deadline = started + budget(request).toNanos();
        CanonicalUrl url = canonicalizer.canonicalize(request.url());
        FetchRequest hop = request.withUrl(url.value());

        for (int redirects = 0; ; redirects++) {
            if (request.purpose() != FetchPurpose.ROBOTS) {
                robots.check(url, remaining(deadline));
            }
            FetchResult result = fetchWithPermit(hop, deadline);
            if (!result.isRedirect()) {
                return result.withFinalUrl(url.value(), elapsedMs(started));
            }
            if (redirects >= limits.maxRedirects()) {
                throw SourceFetchFailedException.permanent("The page redirects too many times.");
            }

            url = canonicalizer.canonicalize(resolve(url.value(), result.location()));
            hop = request.withUrl(url.value()).withoutValidators();
        }
    }

    private Duration budget(FetchRequest request) {
        Duration total = limits.totalTimeout();
        return request.timeout() == null || request.timeout().compareTo(total) > 0 ? total : request.timeout();
    }

    private FetchResult fetchWithPermit(FetchRequest hop, long deadline) {
        Duration remaining = remaining(deadline);
        try {
            if (!permits.tryAcquire(remaining.toNanos(), TimeUnit.NANOSECONDS)) {
                throw SourceFetchFailedException.retryable("The fetcher is busy. Try again shortly.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw SourceFetchFailedException.retryable("The fetch was interrupted.");
        }

        try {
            return delegate.fetch(hop.withTimeout(remaining(deadline)));
        } finally {
            permits.release();
        }
    }

    private static Duration remaining(long deadline) {
        long nanos = deadline - System.nanoTime();
        if (nanos <= 0) {
            throw SourceFetchFailedException.retryable("The site did not answer in time.");
        }
        return Duration.ofNanos(nanos);
    }

    private static String resolve(String current, String location) {
        try {
            return URI.create(current).resolve(location.trim()).toString();
        } catch (IllegalArgumentException e) {
            throw SourceFetchFailedException.permanent("The page redirects to an invalid URL.");
        }
    }

    private static long elapsedMs(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }
}
