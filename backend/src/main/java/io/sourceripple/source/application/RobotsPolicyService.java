package io.sourceripple.source.application;

import crawlercommons.robots.BaseRobotRules;
import crawlercommons.robots.SimpleRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;
import io.sourceripple.shared.api.error.ApiException;
import io.sourceripple.source.application.fetch.FetchPurpose;
import io.sourceripple.source.application.fetch.FetchRequest;
import io.sourceripple.source.application.fetch.FetchResult;
import io.sourceripple.source.application.fetch.WebFetcher;
import io.sourceripple.source.application.fetch.failure.RobotsDisallowedException;
import io.sourceripple.source.application.fetch.failure.SourceFetchFailedException;
import io.sourceripple.source.domain.CanonicalUrl;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class RobotsPolicyService {

    public static final String ROBOT_NAME = "sourceripplebot";

    static final int MAX_CACHED_ORIGINS = 1_000;

    private static final Duration CACHE_FOR = Duration.ofHours(24);
    private static final Duration RETRY_UNREACHABLE_AFTER = Duration.ofHours(1);

    private final WebFetcher fetcher;
    private final Clock clock;
    private final SimpleRobotRulesParser parser = new SimpleRobotRulesParser();
    private final Map<String, CachedRules> cache = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CachedRules> eldest) {
            return size() > MAX_CACHED_ORIGINS || !eldest.getValue().expiresAt().isAfter(clock.instant());
        }
    };

    // The robots request travels through the same safe fetcher that consults this service.
    public RobotsPolicyService(@Lazy WebFetcher fetcher, Clock clock) {
        this.fetcher = fetcher;
        this.clock = clock;
    }

    public void check(CanonicalUrl url, Duration budget) {
        BaseRobotRules rules = rulesFor(url, budget);
        if (rules.isDeferVisits()) {
            throw SourceFetchFailedException.retryable("robots.txt for this site is unavailable. Try again later.");
        }
        if (!rules.isAllowed(url.value())) {
            throw new RobotsDisallowedException(url.value());
        }
    }

    int cachedOrigins() {
        synchronized (cache) {
            return cache.size();
        }
    }

    private BaseRobotRules rulesFor(CanonicalUrl url, Duration budget) {
        Instant now = clock.instant();
        synchronized (cache) {
            CachedRules cached = cache.get(url.origin());
            if (cached != null && cached.expiresAt().isAfter(now)) {
                return cached.rules();
            }
        }

        CachedRules fresh = load(url.origin(), budget, now);
        synchronized (cache) {
            cache.put(url.origin(), fresh);
        }
        return fresh.rules();
    }

    private CachedRules load(String origin, Duration budget, Instant now) {
        String robotsUrl = origin + "/robots.txt";
        FetchResult result;
        try {
            result = fetcher.fetch(FetchRequest.get(robotsUrl, FetchPurpose.ROBOTS).withTimeout(budget));
        } catch (ApiException unreachable) {
            // An unreachable robots.txt does not forbid crawling; try again sooner than a day.
            return new CachedRules(
                    new SimpleRobotRules(SimpleRobotRules.RobotRulesMode.ALLOW_ALL),
                    now.plus(RETRY_UNREACHABLE_AFTER));
        }

        if (result.isSuccess()) {
            BaseRobotRules rules =
                    parser.parseContent(robotsUrl, result.body(), result.contentType(), List.of(ROBOT_NAME));
            return new CachedRules(rules, now.plus(CACHE_FOR));
        }
        BaseRobotRules failed = parser.failedFetch(result.status());
        return new CachedRules(failed, now.plus(failed.isDeferVisits() ? RETRY_UNREACHABLE_AFTER : CACHE_FOR));
    }

    private record CachedRules(BaseRobotRules rules, Instant expiresAt) {}
}
