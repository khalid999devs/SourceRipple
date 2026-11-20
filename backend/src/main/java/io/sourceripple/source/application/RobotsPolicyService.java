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
import io.sourceripple.source.domain.CanonicalUrl;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class RobotsPolicyService {

    public static final String ROBOT_NAME = "sourceripplebot";

    private static final Duration CACHE_FOR = Duration.ofHours(24);
    private static final Duration RETRY_UNREACHABLE_AFTER = Duration.ofHours(1);

    private final WebFetcher fetcher;
    private final Clock clock;
    private final SimpleRobotRulesParser parser = new SimpleRobotRulesParser();
    private final Map<String, CachedRules> cache = new ConcurrentHashMap<>();

    // The robots request travels through the same safe fetcher that consults this service.
    public RobotsPolicyService(@Lazy WebFetcher fetcher, Clock clock) {
        this.fetcher = fetcher;
        this.clock = clock;
    }

    public void check(CanonicalUrl url) {
        if (!rulesFor(url).isAllowed(url.value())) {
            throw new RobotsDisallowedException(url.value());
        }
    }

    private BaseRobotRules rulesFor(CanonicalUrl url) {
        Instant now = clock.instant();
        CachedRules cached = cache.get(url.origin());
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.rules();
        }
        CachedRules fresh = load(url.origin(), now);
        cache.put(url.origin(), fresh);
        return fresh.rules();
    }

    private CachedRules load(String origin, Instant now) {
        String robotsUrl = origin + "/robots.txt";
        try {
            FetchResult result = fetcher.fetch(FetchRequest.get(robotsUrl, FetchPurpose.ROBOTS));
            if (result.isSuccess()) {
                BaseRobotRules rules =
                        parser.parseContent(robotsUrl, result.body(), result.contentType(), List.of(ROBOT_NAME));
                return new CachedRules(rules, now.plus(CACHE_FOR));
            }
            return new CachedRules(parser.failedFetch(result.status()), now.plus(CACHE_FOR));
        } catch (ApiException unreachable) {
            // A missing or unreachable robots.txt does not forbid crawling; try again sooner than a day.
            return new CachedRules(
                    new SimpleRobotRules(SimpleRobotRules.RobotRulesMode.ALLOW_ALL),
                    now.plus(RETRY_UNREACHABLE_AFTER));
        }
    }

    private record CachedRules(BaseRobotRules rules, Instant expiresAt) {}
}
